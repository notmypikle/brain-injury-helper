from typing import List

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

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
    steps: List[str] = []


def rows(cursor):
    return [dict(r) for r in cursor.fetchall()]


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
    # Placeholder until an LLM provider is wired in.
    return ChatResponse(reply=f"I hear you. You said: {req.message}")
