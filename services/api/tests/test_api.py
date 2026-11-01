import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("BIH_DB_PATH", str(tmp_path / "test.db"))
    from app.main import app

    return TestClient(app)


def test_health(client):
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json() == {"status": "ok"}


def test_chat_echo(client):
    r = client.post("/chat", json={"message": "hello"})
    assert r.status_code == 200
    body = r.json()
    assert "hello" in body["reply"]
    assert body["safety_flag"] is False


def test_chat_crisis_detection(client):
    r = client.post("/chat", json={"message": "I want to die"})
    assert r.status_code == 200
    body = r.json()
    assert body["safety_flag"] is True
    assert "988" in body["reply"]


def test_chat_rejects_empty_message(client):
    assert client.post("/chat", json={"message": ""}).status_code == 422


def test_chat_rejects_oversized_message(client):
    r = client.post("/chat", json={"message": "x" * 4001})
    assert r.status_code == 422


def test_notes_roundtrip(client):
    r = client.post("/notes", json={"text": "Dr. appointment Tuesday"})
    assert r.status_code == 201
    note = r.json()
    assert note["text"] == "Dr. appointment Tuesday"
    assert note["id"] >= 1

    r = client.get("/notes")
    assert r.status_code == 200
    assert any(n["id"] == note["id"] for n in r.json())

    assert client.delete(f"/notes/{note['id']}").status_code == 204
    assert all(n["id"] != note["id"] for n in client.get("/notes").json())


def test_delete_missing_note_returns_404(client):
    assert client.delete("/notes/9999").status_code == 404
