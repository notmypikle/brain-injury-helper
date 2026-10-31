import json
import os
from typing import List

import httpx
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, Field, ValidationError

from .db import get_conn, init_db

app = FastAPI(title="BIH API", version="0.1.0")
init_db()


class NoteIn(BaseModel):
    text: str


class ReminderIn(BaseModel):
    text: str
    due_at: str  # ISO time, e.g. 2026-11-01T09:00


class RoutineIn(BaseModel):
    name: str
    steps: List[str] = Field(default_factory=list)


class PaperworkRequest(BaseModel):
    text: str


class PaperworkResponse(BaseModel):
    plain_summary: str
    key_actions: List[str]
    deadlines: List[str]
    questions_to_ask: List[str]
    safety_note: str


OPENAI_MODEL = os.environ.get("OPENAI_MODEL", "gpt-4o-mini")
OPENAI_URL = "https://api.openai.com/v1/chat/completions"


def rows(cursor):
    return [dict(r) for r in cursor.fetchall()]


def _require_openai_key() -> str:
    key = os.environ.get("OPENAI_API_KEY", "").strip()
    if not key:
        raise HTTPException(
            status_code=503,
            detail="OPENAI_API_KEY is not set on the server.",
        )
    return key


def _openai_chat(messages: List[dict], *, json_mode: bool = False) -> str:
    key = _require_openai_key()
    body = {"model": OPENAI_MODEL, "messages": messages, "temperature": 0.2}
    if json_mode:
        body["response_format"] = {"type": "json_object"}

    headers = {"Authorization": f"Bearer {key}", "Content-Type": "application/json"}
    try:
        with httpx.Client(timeout=45.0) as client:
            resp = client.post(OPENAI_URL, headers=headers, json=body)
            resp.raise_for_status()
    except httpx.TimeoutException as exc:
        raise HTTPException(status_code=504, detail="OpenAI request timed out.") from exc
    except httpx.HTTPStatusError as exc:
        detail = "OpenAI request failed."
        if exc.response.status_code in (401, 403):
            detail = "OpenAI authentication failed. Check OPENAI_API_KEY."
        raise HTTPException(status_code=502, detail=detail) from exc
    except httpx.HTTPError as exc:
        raise HTTPException(status_code=502, detail="OpenAI connection failed.") from exc

    data = resp.json()
    choices = data.get("choices", [])
    if not choices:
        raise HTTPException(status_code=502, detail="OpenAI returned no choices.")

    content = choices[0].get("message", {}).get("content", "").strip()
    if not content:
        raise HTTPException(status_code=502, detail="OpenAI returned empty content.")
    return content


@app.post("/notes", status_code=201)
def add_note(note: NoteIn):
    with get_conn() as c:
        cur = c.execute("INSERT INTO notes (text) VALUES (?)", (note.text,))
        return {"id": cur.lastrowid, "text": note.text}


@app.get("/notes")
def list_notes(q: str = ""):
    with get_conn() as c:
        return rows(c.execute(
            "SELECT * FROM notes WHERE text LIKE ? ORDER BY id DESC", (f"%{q}%",)))


@app.delete("/notes/{note_id}")
def delete_note(note_id: int):
    with get_conn() as c:
        if c.execute("DELETE FROM notes WHERE id = ?", (note_id,)).rowcount == 0:
            raise HTTPException(404, "Note not found")
    return {"deleted": note_id}


@app.post("/reminders", status_code=201)
def add_reminder(r: ReminderIn):
    with get_conn() as c:
        cur = c.execute(
            "INSERT INTO reminders (text, due_at) VALUES (?, ?)", (r.text, r.due_at))
        return {"id": cur.lastrowid, **r.dict(), "done": 0}


@app.get("/reminders")
def list_reminders(include_done: bool = False):
    sql = "SELECT * FROM reminders"
    if not include_done:
        sql += " WHERE done = 0"
    with get_conn() as c:
        return rows(c.execute(sql + " ORDER BY due_at"))


@app.post("/reminders/{reminder_id}/done")
def complete_reminder(reminder_id: int):
    with get_conn() as c:
        cur = c.execute("UPDATE reminders SET done = 1 WHERE id = ?", (reminder_id,))
        if cur.rowcount == 0:
            raise HTTPException(404, "Reminder not found")
    return {"done": reminder_id}


@app.post("/routines", status_code=201)
def add_routine(r: RoutineIn):
    with get_conn() as c:
        rid = c.execute("INSERT INTO routines (name) VALUES (?)", (r.name,)).lastrowid
        c.executemany(
            "INSERT INTO routine_steps (routine_id, position, text) VALUES (?, ?, ?)",
            [(rid, i, s) for i, s in enumerate(r.steps)])
    return {"id": rid, "name": r.name, "steps": r.steps}


@app.get("/routines")
def list_routines():
    with get_conn() as c:
        routines = rows(c.execute("SELECT * FROM routines ORDER BY id"))
        for r in routines:
            r["steps"] = [s["text"] for s in c.execute(
                "SELECT text FROM routine_steps WHERE routine_id = ? ORDER BY position",
                (r["id"],))]
        return routines


@app.delete("/routines/{routine_id}")
def delete_routine(routine_id: int):
    with get_conn() as c:
        if c.execute("DELETE FROM routines WHERE id = ?", (routine_id,)).rowcount == 0:
            raise HTTPException(404, "Routine not found")
    return {"deleted": routine_id}


class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    reply: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    if not req.message.strip():
        raise HTTPException(400, "Message is required")

    system_prompt = (
        "You are B.I.H., a calm assistant for people with brain injuries. "
        "Use plain language, short steps, and gentle tone. "
        "Do not claim legal or medical authority. Keep replies concise."
    )
    reply = _openai_chat(
        [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": req.message},
        ]
    )
    return ChatResponse(reply=reply)


@app.post("/paperwork/analyze", response_model=PaperworkResponse)
def analyze_paperwork(req: PaperworkRequest):
    if not req.text.strip():
        raise HTTPException(400, "Paperwork text is required")

    system_prompt = (
        "You are B.I.H. Paperwork Helper. Return JSON only with keys: "
        "plain_summary (string), key_actions (array of short strings), "
        "deadlines (array of short strings), questions_to_ask (array of short strings), "
        "safety_note (string). "
        "Use plain language for cognitive accessibility. Keep each item short. "
        "If a field is unknown, return a safe, explicit message in that field."
    )
    raw = _openai_chat(
        [
            {"role": "system", "content": system_prompt},
            {
                "role": "user",
                "content": (
                    "Analyze this paperwork text and extract what matters:\n\n"
                    f"{req.text}"
                ),
            },
        ],
        json_mode=True,
    )
    try:
        parsed = json.loads(raw)
    except json.JSONDecodeError as exc:
        raise HTTPException(status_code=502, detail="OpenAI returned invalid JSON.") from exc

    try:
        return PaperworkResponse(**parsed)
    except ValidationError as exc:
        raise HTTPException(
            status_code=502,
            detail="OpenAI response JSON was missing required fields.",
        ) from exc
