"""Tests for note folders (user request 2026-11-11: save into a folder
of my choosing) and the notes-table migration for existing phones."""

import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("BIH_DB_PATH", str(tmp_path / "test.db"))
    from app.main import app

    return TestClient(app)


def test_note_with_folder(client):
    r = client.post("/notes", json={"text": "NHS number 123", "folder": "Health"})
    assert r.status_code == 201
    assert r.json()["folder"] == "Health"
    assert client.get("/notes").json()[0]["folder"] == "Health"


def test_folder_defaults_to_empty(client):
    r = client.post("/notes", json={"text": "no folder here"})
    assert r.status_code == 201
    assert r.json()["folder"] == ""


def test_folders_endpoint_lists_unique_folders(client):
    client.post("/notes", json={"text": "a", "folder": "Legal"})
    client.post("/notes", json={"text": "b", "folder": "Health"})
    client.post("/notes", json={"text": "c", "folder": "Legal"})
    client.post("/notes", json={"text": "d"})  # unfiled — not a folder
    assert client.get("/notes/folders").json() == ["Health", "Legal"]


def test_folder_rejects_unsafe_names(client):
    r = client.post("/notes", json={"text": "x", "folder": "bad\nname"})
    assert r.status_code == 422
    r = client.post("/notes", json={"text": "x", "folder": "a" * 61})
    assert r.status_code == 422


def test_existing_db_without_folder_column_migrates(tmp_path, monkeypatch):
    """The user's phone already has a notes table without `folder` —
    opening the DB must add the column without losing anything."""
    import sqlite3

    db_file = tmp_path / "old.db"
    conn = sqlite3.connect(db_file)
    conn.execute(
        "CREATE TABLE notes (id INTEGER PRIMARY KEY AUTOINCREMENT,"
        " text TEXT NOT NULL,"
        " created_at TEXT NOT NULL DEFAULT (datetime('now')))"
    )
    conn.execute("INSERT INTO notes (text) VALUES ('precious memory')")
    conn.commit()
    conn.close()

    monkeypatch.setenv("BIH_DB_PATH", str(db_file))
    from app.main import app

    c = TestClient(app)
    notes = c.get("/notes").json()
    assert notes[0]["text"] == "precious memory"  # nothing lost
    assert notes[0]["folder"] == ""  # column added with a safe default
    assert c.post("/notes", json={"text": "new", "folder": "Health"}).status_code == 201


def test_whole_screen_capture_fits_in_a_note(client):
    # Real use 2026-11-11: saving a full page hit the old 4000-char cap.
    page = "Read from screen:\n" + ("A long document line. " * 400)  # ~8.8k chars
    r = client.post("/notes", json={"text": page, "folder": "Little Beast Enterprise"})
    assert r.status_code == 201
    assert r.json()["text"] == page


def test_notes_still_have_a_sane_limit(client):
    r = client.post("/notes", json={"text": "x" * 50001})
    assert r.status_code == 422


def test_documentation_sized_note_saves(client):
    # Heavy record-keeping use (2026-11-11): ~20k chars, foldered.
    r = client.post("/notes", json={"text": "entry " * 3500, "folder": "Documentation"})
    assert r.status_code == 201
