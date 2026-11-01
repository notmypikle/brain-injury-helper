"""Local-first SQLite storage for B.I.H.

The database location can be overridden with the BIH_DB_PATH environment
variable (useful for tests and for running on Android/Termux).
"""

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
