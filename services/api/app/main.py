import re
from contextlib import closing

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field

from . import db

app = FastAPI(title="BIH API", version="0.2.0")

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


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    if CRISIS_PATTERN.search(req.message):
        return ChatResponse(reply=CRISIS_REPLY, safety_flag=True)
    # Placeholder until an LLM provider is wired in.
    return ChatResponse(reply=f"I hear you. You said: {req.message}")


@app.post("/notes", response_model=Note, status_code=201)
def create_note(note: NoteIn):
    with closing(db.connect()) as conn:
        return db.add_note(conn, note.text)


@app.get("/notes", response_model=list[Note])
def get_notes():
    with closing(db.connect()) as conn:
        return db.list_notes(conn)


@app.delete("/notes/{note_id}", status_code=204)
def remove_note(note_id: int):
    with closing(db.connect()) as conn:
        if not db.delete_note(conn, note_id):
            raise HTTPException(status_code=404, detail="Note not found")
