import json
import os
import re
import secrets
from contextlib import closing
from datetime import datetime

from fastapi import Depends, FastAPI, HTTPException, Request
from fastapi.responses import PlainTextResponse
from pydantic import BaseModel, Field

from . import assistant, calm, db, paperwork

app = FastAPI(title="BIH API", version="0.4.0")


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


# Folder names for notes (user request 2026-11-11: "saved in a folder of
# my choosing"). Friendly names only — letters, digits, spaces and a few
# safe marks; empty means unfiled.
FOLDER_PATTERN = re.compile(r"^[\w \-&',.]{0,60}$")


class NoteIn(BaseModel):
    text: str = Field(min_length=1, max_length=4000)
    folder: str = Field(default="", max_length=60)


class Note(BaseModel):
    id: int
    text: str
    folder: str = ""
    created_at: str


class ReminderIn(BaseModel):
    text: str = Field(min_length=1, max_length=1000)
    # ISO 8601 or friendly words ("6 pm", "in 30 minutes") — _parse_due_at
    # validates, so the model itself stays permissive.
    due_at: str = Field(min_length=1, max_length=40)


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
    # Real local assistant (replaced the placeholder echo after user
    # feedback 2026-11-10). Uses notes/reminders/routines; no network.
    with closing(db.connect()) as conn:
        return ChatResponse(reply=assistant.respond(req.message, conn))


@app.post("/notes", response_model=Note, status_code=201,
          dependencies=[Depends(require_api_key)])
def create_note(note: NoteIn):
    folder = note.folder.strip()
    if not FOLDER_PATTERN.match(folder):
        raise HTTPException(
            status_code=422,
            detail="Folder names can use letters, numbers, spaces and - & ' , . only.",
        )
    with closing(db.connect()) as conn:
        return db.add_note(conn, note.text, folder)


@app.get("/notes/folders", response_model=list[str],
         dependencies=[Depends(require_api_key)])
def get_note_folders():
    """Folder names in use — lets the app offer one-tap folder picks."""
    with closing(db.connect()) as conn:
        return db.list_note_folders(conn)


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

def _parse_due_at(due_at: str) -> str:
    """Accept ISO 8601 (preferred: explicit UTC offset, naive treated as
    local time — risk P2) OR friendly words like '6 pm' / 'in 30 minutes'
    (user feedback 2026-11-11: raw ISO caused a 422 in real use).
    Returns the normalized naive-local ISO string."""
    try:
        datetime.fromisoformat(due_at)
        return due_at
    except ValueError:
        pass
    friendly = assistant.parse_due(due_at)
    if friendly is None:
        raise HTTPException(
            status_code=422,
            detail="I didn't understand that time. Try \"6 pm\", "
                   "\"in 30 minutes\", \"tomorrow 9 am\", or 2026-11-11T18:00.",
        )
    return friendly.strftime("%Y-%m-%dT%H:%M")


@app.post("/reminders", response_model=Reminder, status_code=201,
          dependencies=[Depends(require_api_key)])
def create_reminder(reminder: ReminderIn):
    normalized = _parse_due_at(reminder.due_at)
    with closing(db.connect()) as conn:
        return db.add_reminder(conn, reminder.text, normalized)


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


# --- Calm mode ("Too much") ---------------------------------------------------
# Overstimulation support (user's core accessibility need, 2026-11-10).
# Static grounding steps — the app also bundles a copy so calm mode works
# even when the API can't be reached.

class CalmSteps(BaseModel):
    steps: list[str]


@app.get("/calm/steps", response_model=CalmSteps,
         dependencies=[Depends(require_api_key)])
def calm_steps():
    return CalmSteps(steps=calm.STEPS)


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


# --- Paperwork wizard (Phase 3) ---------------------------------------------
# For forms that can only be filed online: store personal details once
# (profile), pick a template, answer the remaining questions one at a time,
# and get a finished document whose lines are easy to copy into a web form.

PROFILE_KEY_PATTERN = re.compile(r"^[a-z0-9_]{1,60}$")


class ProfileUpdate(BaseModel):
    fields: dict[str, str] = Field(min_length=1, max_length=100)


class TemplateField(BaseModel):
    key: str
    label: str
    auto: bool


class PaperworkTemplate(BaseModel):
    id: str
    title: str
    description: str
    fields: list[TemplateField]


class FillRequest(BaseModel):
    template_id: str = Field(min_length=1, max_length=60)
    values: dict[str, str] = Field(default_factory=dict, max_length=100)


class PaperworkDocument(BaseModel):
    id: int
    template_id: str
    title: str
    content: str
    missing_fields: list[str]
    created_at: str


def _validate_profile_fields(fields: dict[str, str]) -> None:
    for key, value in fields.items():
        if not PROFILE_KEY_PATTERN.match(key):
            raise HTTPException(
                status_code=422,
                detail=f"Invalid field name: {key!r}. Use lowercase letters, digits, underscores.",
            )
        if len(value) > 2000:
            raise HTTPException(status_code=422, detail=f"Value for {key!r} is too long (max 2000 characters).")


@app.get("/profile", response_model=dict[str, str],
         dependencies=[Depends(require_api_key)])
def get_profile():
    with closing(db.connect()) as conn:
        return db.get_profile(conn)


@app.put("/profile", response_model=dict[str, str],
         dependencies=[Depends(require_api_key)])
def update_profile(update: ProfileUpdate):
    _validate_profile_fields(update.fields)
    with closing(db.connect()) as conn:
        return db.set_profile_fields(conn, update.fields)


@app.delete("/profile/{key}", status_code=204,
            dependencies=[Depends(require_api_key)])
def remove_profile_field(key: str):
    with closing(db.connect()) as conn:
        if not db.delete_profile_field(conn, key):
            raise HTTPException(status_code=404, detail="Profile field not found")


@app.get("/paperwork/templates", response_model=list[PaperworkTemplate],
         dependencies=[Depends(require_api_key)])
def list_templates():
    return paperwork.TEMPLATES


@app.post("/paperwork/fill", response_model=PaperworkDocument, status_code=201,
          dependencies=[Depends(require_api_key)])
def fill_paperwork(req: FillRequest):
    template = paperwork.get_template(req.template_id)
    if template is None:
        raise HTTPException(status_code=404, detail="Unknown template")
    _validate_profile_fields(req.values)
    with closing(db.connect()) as conn:
        profile = db.get_profile(conn)
        content, missing = paperwork.render(template, profile, req.values)
        doc = db.add_document(conn, template["id"], template["title"], content, missing)
    return _document_response(doc)


@app.get("/paperwork/documents", response_model=list[PaperworkDocument],
         dependencies=[Depends(require_api_key)])
def get_documents():
    with closing(db.connect()) as conn:
        return [_document_response(d) for d in db.list_documents(conn)]


@app.get("/paperwork/documents/{document_id}", response_model=PaperworkDocument,
         dependencies=[Depends(require_api_key)])
def get_document(document_id: int):
    with closing(db.connect()) as conn:
        doc = db.get_document(conn, document_id)
        if doc is None:
            raise HTTPException(status_code=404, detail="Document not found")
        return _document_response(doc)


@app.get("/paperwork/documents/{document_id}/export",
         dependencies=[Depends(require_api_key)])
def export_document(document_id: int):
    with closing(db.connect()) as conn:
        doc = db.get_document(conn, document_id)
        if doc is None:
            raise HTTPException(status_code=404, detail="Document not found")
    return PlainTextResponse(
        doc["content"],
        headers={
            "Content-Disposition": f'attachment; filename="document-{document_id}.txt"'
        },
    )


@app.delete("/paperwork/documents/{document_id}", status_code=204,
            dependencies=[Depends(require_api_key)])
def remove_document(document_id: int):
    with closing(db.connect()) as conn:
        if not db.delete_document(conn, document_id):
            raise HTTPException(status_code=404, detail="Document not found")


def _document_response(doc: dict) -> PaperworkDocument:
    return PaperworkDocument(
        id=doc["id"],
        template_id=doc["template_id"],
        title=doc["title"],
        content=doc["content"],
        missing_fields=doc["missing"],
        created_at=doc["created_at"],
    )


# --- Data rights (Phase 4 launch gate): export everything, delete everything ---

@app.get("/export", dependencies=[Depends(require_api_key)])
def export_all():
    """Download every piece of stored data as one JSON file (backup / GDPR-style
    portability, risks S8 + O1)."""
    with closing(db.connect()) as conn:
        data = db.export_all(conn)
    return PlainTextResponse(
        json.dumps(data, indent=2),
        media_type="application/json",
        headers={"Content-Disposition": 'attachment; filename="bih-export.json"'},
    )


@app.delete("/data", dependencies=[Depends(require_api_key)])
def delete_all_data():
    """Delete EVERYTHING: notes, reminders, routines, profile, documents.
    There is no undo (risk S8)."""
    with closing(db.connect()) as conn:
        db.wipe_all(conn)
    return {"deleted": True}
