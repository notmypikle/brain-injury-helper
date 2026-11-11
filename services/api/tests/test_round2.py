"""Round-2 real-use requests (2026-11-11): note editing, folder recall,
paperwork checklists, routine suggestions."""

import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("BIH_DB_PATH", str(tmp_path / "test.db"))
    from app.main import app

    return TestClient(app)


def test_edit_note_text_and_folder(client):
    n = client.post("/notes", json={"text": "draft", "folder": "Health"}).json()
    r = client.put(f"/notes/{n['id']}", json={"text": "corrected", "folder": "Legal"})
    assert r.status_code == 200
    assert r.json()["text"] == "corrected" and r.json()["folder"] == "Legal"


def test_edit_missing_note_404(client):
    assert client.put("/notes/999", json={"text": "x"}).status_code == 404


def test_edit_note_folder_validated(client):
    n = client.post("/notes", json={"text": "keep"}).json()
    assert client.put(f"/notes/{n['id']}", json={"text": "x", "folder": "bad\nname"}).status_code == 422


def test_whats_in_my_folder(client):
    client.post("/notes", json={"text": "lease signed", "folder": "Housing"})
    r = client.post("/chat", json={"message": "what's in my Housing folder?"})
    assert r.status_code == 200 and "lease signed" in r.json()["reply"]


def test_folder_recall_empty(client):
    r = client.post("/chat", json={"message": "what's in my Legal folder?"})
    assert r.status_code == 200 and r.json()["safety_flag"] is False


def test_checklists_endpoint(client):
    r = client.get("/paperwork/checklists")
    assert r.status_code == 200
    titles = [c["title"] for c in r.json()]
    assert any("benefits" in t.lower() for t in titles)
    for c in r.json():
        assert c["forms"] and c["gathers"] and c["note"]


def test_routine_suggestion_known_name(client):
    r = client.get("/routines/suggest", params={"name": "morning"})
    assert r.status_code == 200
    body = r.json()
    assert body["matched"] is True and len(body["steps"]) >= 3


def test_routine_suggestion_unknown_name(client):
    r = client.get("/routines/suggest", params={"name": "zzzz"})
    assert r.json()["matched"] is False
    assert len(r.json()["steps"]) > 0  # still gets a gentle starter
