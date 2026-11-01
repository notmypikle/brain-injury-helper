import os
import re
import secrets
from contextlib import closing

from fastapi import Depends, FastAPI, HTTPException, Request
from pydantic import BaseModel, Field

from . import db

app = FastAPI(title="BIH API", version="0.3.0")


async def require_api_key(request: Request) -> None:
    """Optional shared-secret protection.

    Set BIH_API_KEY to require every request (except /health) to send
    the matching key in the X-API-Key header. Leave unset for local use.
    """
    expected = os.environ.get("BIH_API_KEY")
    if not expected:
        return
    provided = request.headers.get("X-API-Key", "")
    if not secrets.compare_digest(provided, expected):
        raise HTTPException(status_code=401, detail="Invalid or missing API key")

# Safety boundary from docs/PRIVACY_AND_SAFETY.md: if the user may be in
# danger, respond with care and point to real crisis help.
CRISIS_PATTERN = re.compile(
    r"\b(kill(ing)? (myself|me)|suicid\w*|end(ing)? (my|it) (life|all)"
    r"|want(ing)? to die|self[- ]?harm|hurt(ing)? myself)\b",
    re.IGNORECASE,
)

CRISIS_REPLY = (
    "I'm really glad you told me that. Your safety matters most right now. "
    "Please reach out to someone who can help right away: call your local "
    "emergency number, or a crisis line — in the US you can call or text 988 "
    "(Suicide & Crisis Lifeline). If you can, tell someone near you how "
    "you're feeling. I'm here with you."
)


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)


class ChatResponse(BaseModel):
    reply: str
    safety_flag: bool = False


class NoteIn(BaseModel):
    text: str = Field(min_length=1, max_length=4000)


class Note(BaseModel):
    id: int
    text: str
    created_at: str


class ReminderIn(BaseModel):
    text: str = Field(min_length=1, max_length=1000)
    # ISO 8601 datetime, e.g. "2026-11-02T09:00"
    due_at: str = Field(min_length=10, max_length=40)


class Reminder(BaseModel):
    id: int
    text: str
    due_at: str
    done: bool
    created_at: str


class RoutineIn(BaseModel):
    name: str = Field(min_length=1, max_length=200)
    steps: list[str] = Field(min_length=1, max_length=50)


class Routine(BaseModel):
    id: int
    name: str
    steps: list[str]
    created_at: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse, dependencies=[Depends(require_api_key)])
def chat(req: ChatRequest):
    if CRISIS_PATTERN.search(req.message):
        return ChatResponse(reply=CRISIS_REPLY, safety_flag=True)
    # Placeholder until an LLM provider is wired in.
    return ChatResponse(reply=f"I hear you. You said: {req.message}")


@app.post("/notes", response_model=Note, status_code=201,
          dependencies=[Depends(require_api_key)])
def create_note(note: NoteIn):
    with closing(db.connect()) as conn:
        return db.add_note(conn, note.text)


@app.get("/notes", response_model=list[Note],
         dependencies=[Depends(require_api_key)])
def get_notes():
    with closing(db.connect()) as conn:
        return db.list_notes(conn)


@app.delete("/notes/{note_id}", status_code=204,
            dependencies=[Depends(require_api_key)])
def remove_note(note_id: int):
    with closing(db.connect()) as conn:
        if not db.delete_note(conn, note_id):
            raise HTTPException(status_code=404, detail="Note not found")


# --- Reminders (Phase 1) ----------------------------------------------------

@app.post("/reminders", response_model=Reminder, status_code=201,
          dependencies=[Depends(require_api_key)])
def create_reminder(reminder: ReminderIn):
    with closing(db.connect()) as conn:
        return db.add_reminder(conn, reminder.text, reminder.due_at)


@app.get("/reminders", response_model=list[Reminder],
         dependencies=[Depends(require_api_key)])
def get_reminders(include_done: bool = False):
    with closing(db.connect()) as conn:
        return db.list_reminders(conn, include_done=include_done)


@app.post("/reminders/{reminder_id}/done", response_model=Reminder,
          dependencies=[Depends(require_api_key)])
def mark_reminder_done(reminder_id: int):
    with closing(db.connect()) as conn:
        reminder = db.complete_reminder(conn, reminder_id)
        if reminder is None:
            raise HTTPException(status_code=404, detail="Reminder not found")
        return reminder


@app.delete("/reminders/{reminder_id}", status_code=204,
            dependencies=[Depends(require_api_key)])
def remove_reminder(reminder_id: int):
    with closing(db.connect()) as conn:
        if not db.delete_reminder(conn, reminder_id):
            raise HTTPException(status_code=404, detail="Reminder not found")


# --- Routines (Phase 1) -----------------------------------------------------

@app.post("/routines", response_model=Routine, status_code=201,
          dependencies=[Depends(require_api_key)])
def create_routine(routine: RoutineIn):
    with closing(db.connect()) as conn:
        return db.add_routine(conn, routine.name, routine.steps)


@app.get("/routines", response_model=list[Routine],
         dependencies=[Depends(require_api_key)])
def get_routines():
    with closing(db.connect()) as conn:
        return db.list_routines(conn)


@app.delete("/routines/{routine_id}", status_code=204,
            dependencies=[Depends(require_api_key)])
def remove_routine(routine_id: int):
    with closing(db.connect()) as conn:
        if not db.delete_routine(conn, routine_id):
            raise HTTPException(status_code=404, detail="Routine not found")


# --- Voice (Phase 3) --------------------------------------------------------
# Talk-to-text and text-to-speech run ON THE DEVICE (Android SpeechRecognizer
# / TextToSpeech APIs) for privacy and zero cost — audio never leaves the
# phone. The API only ever sees text. These endpoints let voice clients use
# one call to send transcribed speech and get a spoken-ready reply.

class VoiceRequest(BaseModel):
    transcript: str = Field(min_length=1, max_length=4000)


class VoiceResponse(BaseModel):
    reply_text: str
    safety_flag: bool = False


@app.post("/voice", response_model=VoiceResponse,
          dependencies=[Depends(require_api_key)])
def voice(req: VoiceRequest):
    """Single round-trip for voice clients: transcribed speech in, reply
    text out (the device speaks it via its own TTS engine)."""
    result = chat(ChatRequest(message=req.transcript))
    return VoiceResponse(reply_text=result.reply, safety_flag=result.safety_flag)
