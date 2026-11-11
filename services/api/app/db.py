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

CREATE TABLE IF NOT EXISTS profile (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE IF NOT EXISTS documents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    template_id TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    missing TEXT NOT NULL DEFAULT '[]',
    created_at TEXT NOT NULL DEFAULT (datetime('now'))
);
"""


def db_path() -> Path:
    return Path(os.environ.get("BIH_DB_PATH", str(DEFAULT_DB_PATH)))


def connect(path: Path | None = None) -> sqlite3.Connection:
    """Open a connection and make sure the schema exists."""
    conn = sqlite3.connect(path or db_path())
    conn.row_factory = sqlite3.Row
    # WAL mode: extra crash safety if power is lost mid-write (risk O2).
    conn.execute("PRAGMA journal_mode=WAL")
    conn.executescript(SCHEMA)
    _migrate(conn)
    return conn


def _migrate(conn: sqlite3.Connection) -> None:
    """In-place upgrades for existing databases (CREATE TABLE IF NOT EXISTS
    can't add columns to a table that already exists on the user's phone)."""
    # 2026-11-11: user-chosen folders for notes ("save that section in a
    # folder of my choosing").
    cols = [r[1] for r in conn.execute("PRAGMA table_info(notes)").fetchall()]
    if "folder" not in cols:
        conn.execute("ALTER TABLE notes ADD COLUMN folder TEXT NOT NULL DEFAULT ''")
        conn.commit()


def add_note(conn: sqlite3.Connection, text: str, folder: str = "") -> dict:
    cur = conn.execute(
        "INSERT INTO notes (text, folder) VALUES (?, ?)", (text, folder)
    )
    conn.commit()
    return get_note(conn, cur.lastrowid)


def get_note(conn: sqlite3.Connection, note_id: int) -> dict | None:
    row = conn.execute(
        "SELECT id, text, folder, created_at FROM notes WHERE id = ?", (note_id,)
    ).fetchone()
    return dict(row) if row else None


def list_notes(conn: sqlite3.Connection) -> list[dict]:
    rows = conn.execute(
        "SELECT id, text, folder, created_at FROM notes ORDER BY id DESC"
    ).fetchall()
    return [dict(r) for r in rows]


def update_note(
    conn: sqlite3.Connection, note_id: int, text: str, folder: str
) -> dict | None:
    cur = conn.execute(
        "UPDATE notes SET text = ?, folder = ? WHERE id = ?",
        (text, folder, note_id),
    )
    conn.commit()
    if cur.rowcount == 0:
        return None
    return get_note(conn, note_id)


def list_note_folders(conn: sqlite3.Connection) -> list[str]:
    rows = conn.execute(
        "SELECT DISTINCT folder FROM notes WHERE folder != '' ORDER BY folder"
    ).fetchall()
    return [r["folder"] for r in rows]


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


# --- Profile (single-user personal details used to auto-fill paperwork) ----

def get_profile(conn: sqlite3.Connection) -> dict:
    rows = conn.execute("SELECT key, value FROM profile ORDER BY key").fetchall()
    return {r["key"]: r["value"] for r in rows}


def set_profile_fields(conn: sqlite3.Connection, fields: dict) -> dict:
    for key, value in fields.items():
        conn.execute(
            "INSERT INTO profile (key, value, updated_at)"
            " VALUES (?, ?, datetime('now'))"
            " ON CONFLICT(key) DO UPDATE SET value = excluded.value,"
            " updated_at = excluded.updated_at",
            (key, value),
        )
    conn.commit()
    return get_profile(conn)


def delete_profile_field(conn: sqlite3.Connection, key: str) -> bool:
    cur = conn.execute("DELETE FROM profile WHERE key = ?", (key,))
    conn.commit()
    return cur.rowcount > 0


# --- Documents (filled paperwork) ------------------------------------------

def add_document(
    conn: sqlite3.Connection,
    template_id: str,
    title: str,
    content: str,
    missing: list[str],
) -> dict:
    cur = conn.execute(
        "INSERT INTO documents (template_id, title, content, missing)"
        " VALUES (?, ?, ?, ?)",
        (template_id, title, content, json.dumps(missing)),
    )
    conn.commit()
    return get_document(conn, cur.lastrowid)


def get_document(conn: sqlite3.Connection, document_id: int) -> dict | None:
    row = conn.execute(
        "SELECT id, template_id, title, content, missing, created_at"
        " FROM documents WHERE id = ?",
        (document_id,),
    ).fetchone()
    return _document_dict(row) if row else None


def list_documents(conn: sqlite3.Connection) -> list[dict]:
    rows = conn.execute(
        "SELECT id, template_id, title, content, missing, created_at"
        " FROM documents ORDER BY id DESC"
    ).fetchall()
    return [_document_dict(r) for r in rows]


def delete_document(conn: sqlite3.Connection, document_id: int) -> bool:
    cur = conn.execute("DELETE FROM documents WHERE id = ?", (document_id,))
    conn.commit()
    return cur.rowcount > 0


def _document_dict(row: sqlite3.Row) -> dict:
    d = dict(row)
    d["missing"] = json.loads(d["missing"])
    return d


# --- Export / wipe (privacy rights: your data, portable and deletable) -----

def export_all(conn: sqlite3.Connection) -> dict:
    return {
        "notes": list_notes(conn),
        "reminders": list_reminders(conn, include_done=True),
        "routines": list_routines(conn),
        "profile": get_profile(conn),
        "documents": list_documents(conn),
    }


def wipe_all(conn: sqlite3.Connection) -> None:
    for table in ("notes", "reminders", "routines", "profile", "documents"):
        conn.execute(f"DELETE FROM {table}")
    conn.commit()
