"""Local-first SQLite storage for B.I.H.

The database location can be overridden with the BIH_DB_PATH environment
variable (useful for tests and for running on Android/Termux).
"""

import json
import os
import sqlite3
from pathlib import Path

DEFAULT_DB_PATH = Path(__file__).resolve().parents[2] / "bih.db"

SCHEMA = """
CREATE TABLE IF NOT EXISTS notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    text TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS reminders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    text TEXT NOT NULL,
    due_at TEXT NOT NULL,
    done INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS routines (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    steps TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
"""


def db_path() -> Path:
    return Path(os.environ.get("BIH_DB_PATH", str(DEFAULT_DB_PATH)))


def connect(path: Path | None = None) -> sqlite3.Connection:
    """Open a connection and make sure the schema exists."""
    conn = sqlite3.connect(path or db_path())
    conn.row_factory = sqlite3.Row
    conn.executescript(SCHEMA)
    return conn


def add_note(conn: sqlite3.Connection, text: str) -> dict:
    cur = conn.execute("INSERT INTO notes (text) VALUES (?)", (text,))
    conn.commit()
    return get_note(conn, cur.lastrowid)


def get_note(conn: sqlite3.Connection, note_id: int) -> dict | None:
    row = conn.execute(
        "SELECT id, text, created_at FROM notes WHERE id = ?", (note_id,)
    ).fetchone()
    return dict(row) if row else None


def list_notes(conn: sqlite3.Connection) -> list[dict]:
    rows = conn.execute(
        "SELECT id, text, created_at FROM notes ORDER BY id DESC"
    ).fetchall()
    return [dict(r) for r in rows]


def delete_note(conn: sqlite3.Connection, note_id: int) -> bool:
    cur = conn.execute("DELETE FROM notes WHERE id = ?", (note_id,))
    conn.commit()
    return cur.rowcount > 0


# --- Reminders -------------------------------------------------------------

def add_reminder(conn: sqlite3.Connection, text: str, due_at: str) -> dict:
    cur = conn.execute(
        "INSERT INTO reminders (text, due_at) VALUES (?, ?)", (text, due_at)
    )
    conn.commit()
    return get_reminder(conn, cur.lastrowid)


def get_reminder(conn: sqlite3.Connection, reminder_id: int) -> dict | None:
    row = conn.execute(
        "SELECT id, text, due_at, done, created_at FROM reminders WHERE id = ?",
        (reminder_id,),
    ).fetchone()
    return _reminder_dict(row) if row else None


def list_reminders(
    conn: sqlite3.Connection, include_done: bool = False
) -> list[dict]:
    sql = "SELECT id, text, due_at, done, created_at FROM reminders"
    if not include_done:
        sql += " WHERE done = 0"
    sql += " ORDER BY due_at ASC"
    return [_reminder_dict(r) for r in conn.execute(sql).fetchall()]


def complete_reminder(conn: sqlite3.Connection, reminder_id: int) -> dict | None:
    conn.execute("UPDATE reminders SET done = 1 WHERE id = ?", (reminder_id,))
    conn.commit()
    return get_reminder(conn, reminder_id)


def delete_reminder(conn: sqlite3.Connection, reminder_id: int) -> bool:
    cur = conn.execute("DELETE FROM reminders WHERE id = ?", (reminder_id,))
    conn.commit()
    return cur.rowcount > 0


def _reminder_dict(row: sqlite3.Row) -> dict:
    d = dict(row)
    d["done"] = bool(d["done"])
    return d


# --- Routines --------------------------------------------------------------

def add_routine(conn: sqlite3.Connection, name: str, steps: list[str]) -> dict:
    cur = conn.execute(
        "INSERT INTO routines (name, steps) VALUES (?, ?)",
        (name, json.dumps(steps)),
    )
    conn.commit()
    return get_routine(conn, cur.lastrowid)


def get_routine(conn: sqlite3.Connection, routine_id: int) -> dict | None:
    row = conn.execute(
        "SELECT id, name, steps, created_at FROM routines WHERE id = ?",
        (routine_id,),
    ).fetchone()
    return _routine_dict(row) if row else None


def list_routines(conn: sqlite3.Connection) -> list[dict]:
    rows = conn.execute(
        "SELECT id, name, steps, created_at FROM routines ORDER BY name ASC"
    ).fetchall()
    return [_routine_dict(r) for r in rows]


def delete_routine(conn: sqlite3.Connection, routine_id: int) -> bool:
    cur = conn.execute("DELETE FROM routines WHERE id = ?", (routine_id,))
    conn.commit()
    return cur.rowcount > 0


def _routine_dict(row: sqlite3.Row) -> dict:
    d = dict(row)
    d["steps"] = json.loads(d["steps"])
    return d
