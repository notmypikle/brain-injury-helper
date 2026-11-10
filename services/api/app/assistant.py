"""B.I.H.'s conversational assistant — local, private, free.

USER FEEDBACK (2026-11-10): "B.I.H. only repeats what I asked it. We
definitely have to fix that." This module replaces the placeholder echo
with real help: the assistant USES B.I.H.'s own features (reminders,
notes, routines) and answers in short, calm sentences (low cognitive
load — see docs/IMPLEMENTATION_PLAN.md).

No LLM, no network, no cost. Crisis detection happens BEFORE this module
is called (in main.py) and always wins. An on-device LLM (Gemini Nano /
LiteRT) is queued in PROJECT_STATUS.md as a later upgrade for free-form
conversation; everything here keeps working as the tool-use layer either
way.
"""

import re
import sqlite3
from datetime import datetime, timedelta

from . import calm, db

REMINDER_RE = re.compile(
    r"\b(?:remind me (?:to|about)|remember to) (.+)$", re.IGNORECASE
)
NOTE_RE = re.compile(
    r"\b(?:remember that|note(?: down)?(?: that)?|write down)[:\s]+(.+)$",
    re.IGNORECASE,
)
LIST_DAY_RE = re.compile(
    r"\b(what'?s on|my reminders|to ?do|what do i need to do|my day)\b",
    re.IGNORECASE,
)
NOTES_LIST_RE = re.compile(
    r"\b(my notes|read my notes|what did i ask you to remember|what have i saved)\b",
    re.IGNORECASE,
)
ROUTINE_RE = re.compile(r"(?:my |the )?(\w[\w -]*?) routine", re.IGNORECASE)
WHEN_IS_RE = re.compile(
    r"\b(?:what time is|when is|when'?s) (?:my |the )?(.+?)\??\s*$",
    re.IGNORECASE,
)
# Overstimulation ("it's too much") — a core accessibility need
# (2026-11-10), handled by calm mode. Crisis detection in main.py runs
# BEFORE this and always wins.
OVERLOAD_RE = re.compile(
    r"\b(too much|overwhelm\w*|too loud|too busy|can'?t cope|can'?t think"
    r"|too many (things|people|sounds|noises)|sensory overload"
    r"|shut ?down|meltdown)\b",
    re.IGNORECASE,
)
GREETING_RE = re.compile(
    r"^\s*(hi+|hello+|hey+|good (morning|afternoon|evening))\b", re.IGNORECASE
)
THANKS_RE = re.compile(r"\b(thank|thanks|thx)\b", re.IGNORECASE)
HELP_RE = re.compile(r"\b(help|what can you do)\b", re.IGNORECASE)

IN_MINUTES_RE = re.compile(r"\bin (\d+) (minute|hour)s?\b", re.IGNORECASE)
AT_TIME_RE = re.compile(r"\bat (\d{1,2})(?::(\d{2}))?\s*(am|pm)?\b", re.IGNORECASE)
TOMORROW_RE = re.compile(r"\btomorrow\b", re.IGNORECASE)


def respond(message: str, conn: sqlite3.Connection) -> str:
    """Turn a user message into a helpful reply, using local features."""
    for handler in (
        _overload_intent,
        _reminder_intent,
        _note_intent,
        _when_is_intent,
        _list_day_intent,
        _notes_list_intent,
        _routine_intent,
        _greeting,
        _thanks,
        _help,
    ):
        reply = handler(message, conn)
        if reply is not None:
            return reply
    return _fallback()


# --- Reminders -------------------------------------------------------------

def _reminder_intent(message: str, conn: sqlite3.Connection) -> str | None:
    m = REMINDER_RE.search(message)
    if not m:
        return None
    due, task = _extract_time(m.group(1).strip())
    if due is None:
        return (
            f'Okay — "{task}". When should I remind you? '
            'For example: "in 30 minutes" or "at 6 pm".'
        )
    db.add_reminder(conn, task, due.strftime("%Y-%m-%dT%H:%M"))
    return f"Done! I'll remind you to {task} — {due:%d %b at %H:%M}."


def _extract_time(text: str) -> tuple[datetime | None, str]:
    """Pull a time phrase out of the task text. Returns (due, task)."""
    now = datetime.now()
    due: datetime | None = None

    m = IN_MINUTES_RE.search(text)
    if m:
        amount = int(m.group(1))
        due = now + (timedelta(minutes=amount) if m.group(2).lower().startswith("minute")
                     else timedelta(hours=amount))
        text = (text[: m.start()] + text[m.end():]).strip()

    m = AT_TIME_RE.search(text)
    if m:
        hour = int(m.group(1))
        minute = int(m.group(2) or 0)
        meridiem = (m.group(3) or "").lower()
        if meridiem == "pm" and hour < 12:
            hour += 12
        if meridiem == "am" and hour == 12:
            hour = 0
        if hour <= 23 and minute <= 59:
            due = now.replace(hour=hour, minute=minute, second=0, microsecond=0)
            if due <= now:
                due += timedelta(days=1)
            text = (text[: m.start()] + text[m.end():]).strip()

    if TOMORROW_RE.search(text):
        base = due or now.replace(hour=9, minute=0, second=0, microsecond=0)
        due = base + timedelta(days=1)
        text = TOMORROW_RE.sub("", text).strip()

    return due, re.sub(r"\s{2,}", " ", text).strip(" ,.-")


# --- Notes -----------------------------------------------------------------

def _note_intent(message: str, conn: sqlite3.Connection) -> str | None:
    m = NOTE_RE.search(message)
    if not m:
        return None
    db.add_note(conn, m.group(1).strip())
    return "Saved that to your notes. 📝"


def _notes_list_intent(message: str, conn: sqlite3.Connection) -> str | None:
    if not NOTES_LIST_RE.search(message):
        return None
    notes = db.list_notes(conn)[:5]
    if not notes:
        return "No notes yet. Say \"remember that …\" and I'll keep it safe."
    lines = [f"{i}. {n['text']}" for i, n in enumerate(notes, 1)]
    return "Here's what you've saved:\n" + "\n".join(lines)


def _when_is_intent(message: str, conn: sqlite3.Connection) -> str | None:
    """'What time is my appointment?' → look it up in reminders."""
    m = WHEN_IS_RE.search(message)
    if not m:
        return None
    topic = m.group(1).strip().lower()
    matches = [r for r in db.list_reminders(conn) if topic in r["text"].lower()]
    if matches:
        return f"Your {topic}: {_friendly_due(matches[0]['due_at'])}. I've got it noted. ✅"
    return (f"I don't have anything about \"{topic}\" yet. "
            f"Say \"remind me to …\" and I'll keep it for you.")


# --- Today / reminders overview -------------------------------------------

def _list_day_intent(message: str, conn: sqlite3.Connection) -> str | None:
    if not LIST_DAY_RE.search(message):
        return None
    reminders = db.list_reminders(conn)[:5]
    if not reminders:
        return "Nothing due right now. Nice and calm. 😌"
    lines = [f"• {r['text']} ({_friendly_due(r['due_at'])})" for r in reminders]
    return "Here's what's coming up:\n" + "\n".join(lines)


def _friendly_due(due_at: str) -> str:
    try:
        return datetime.fromisoformat(due_at).strftime("%d %b %H:%M")
    except ValueError:
        return due_at


# --- Routines ---------------------------------------------------------------

def _routine_intent(message: str, conn: sqlite3.Connection) -> str | None:
    m = ROUTINE_RE.search(message)
    if not m:
        return None
    name = m.group(1).strip().lower()
    routines = db.list_routines(conn)
    if name in ("", "my", "s"):
        if not routines:
            return "No routines yet. Make one in the Routines tab and I'll walk you through it."
        names = ", ".join(r["name"] for r in routines)
        return f"Your routines: {names}. Say one of them plus \"routine\" to walk through it."
    for routine in routines:
        if routine["name"].lower() == name:
            steps = "\n".join(f"{i}. {s}" for i, s in enumerate(routine["steps"], 1))
            return f"Your {routine['name']} routine — one step at a time:\n{steps}\nYou've got this. 💪"
    if routines:
        names = ", ".join(r["name"] for r in routines)
        return f"I don't have a routine called \"{name}\". Yours are: {names}."
    return "No routines yet. Make one in the Routines tab and I'll walk you through it."


# --- Overstimulation / calm mode --------------------------------------------

def _overload_intent(message: str, conn: sqlite3.Connection) -> str | None:
    """'It's too much' → start calming immediately + point at calm mode."""
    if not OVERLOAD_RE.search(message):
        return None
    return calm.OVERLOAD_REPLY


# --- Small talk / guidance ---------------------------------------------------

def _greeting(message: str, conn: sqlite3.Connection) -> str | None:
    if not GREETING_RE.search(message):
        return None
    return ("Hello! 😊 What can I do for you — set a reminder, save a note, "
            "walk through a routine, or help with paperwork?")


def _thanks(message: str, conn: sqlite3.Connection) -> str | None:
    if not THANKS_RE.search(message):
        return None
    return "Anytime. I'm always here. 💙"


def _help(message: str, conn: sqlite3.Connection) -> str | None:
    if not HELP_RE.search(message):
        return None
    return ("I can:\n"
            "• Remind you — \"remind me to take meds at 6 pm\"\n"
            "• Remember things — \"remember that my red folder has my forms\"\n"
            "• Show your day — \"what's on today\"\n"
            "• Walk through routines — \"morning routine\"\n"
            "• Help with paperwork — the Forms tab\n"
            "One thing at a time is absolutely fine.")


def _fallback() -> str:
    return ("I'm listening. I can set reminders, save notes, walk through "
            "routines, or help with paperwork — just tell me which you'd like.")
