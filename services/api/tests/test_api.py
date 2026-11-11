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


def test_chat_greeting(client):
    """The placeholder echo was replaced by the real assistant (2026-11-10)."""
    r = client.post("/chat", json={"message": "hello"})
    assert r.status_code == 200
    body = r.json()
    assert "reminder" in body["reply"]  # greeting explains capabilities
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


def test_reminders_roundtrip(client):
    r = client.post(
        "/reminders",
        json={"text": "Take medication", "due_at": "2026-11-02T09:00"},
    )
    assert r.status_code == 201
    rem = r.json()
    assert rem["done"] is False

    r = client.post(f"/reminders/{rem['id']}/done")
    assert r.status_code == 200
    assert r.json()["done"] is True

    # Completed reminders hidden by default, visible with include_done
    assert all(x["id"] != rem["id"] for x in client.get("/reminders").json())
    assert any(
        x["id"] == rem["id"]
        for x in client.get("/reminders?include_done=true").json()
    )

    assert client.delete(f"/reminders/{rem['id']}").status_code == 204
    assert client.delete("/reminders/9999").status_code == 404


def test_reminder_accepts_iso_with_offset(client):
    r = client.post(
        "/reminders",
        json={"text": "Call doctor", "due_at": "2026-11-02T09:00+00:00"},
    )
    assert r.status_code == 201
    assert r.json()["due_at"] == "2026-11-02T09:00+00:00"


def test_reminder_rejects_bad_due_at(client):
    for bad in ["tomorrow morning", "11/02/2026 9am", "2026-13-40T99:99"]:
        r = client.post("/reminders", json={"text": "Nope", "due_at": bad})
        assert r.status_code == 422, bad


def test_routines_roundtrip(client):
    r = client.post(
        "/routines",
        json={"name": "Morning", "steps": ["Wake up", "Medication", "Breakfast"]},
    )
    assert r.status_code == 201
    routine = r.json()
    assert routine["steps"] == ["Wake up", "Medication", "Breakfast"]

    assert any(x["id"] == routine["id"] for x in client.get("/routines").json())

    assert client.delete(f"/routines/{routine['id']}").status_code == 204
    assert client.delete("/routines/9999").status_code == 404


def test_voice_endpoint_roundtrip(client):
    r = client.post("/voice", json={"transcript": "what time is my appointment"})
    assert r.status_code == 200
    body = r.json()
    assert "appointment" in body["reply_text"]
    assert body["safety_flag"] is False


def test_voice_endpoint_crisis(client):
    r = client.post("/voice", json={"transcript": "I want to hurt myself"})
    assert r.status_code == 200
    assert r.json()["safety_flag"] is True


def test_api_key_required_when_configured(client, monkeypatch):
    monkeypatch.setenv("BIH_API_KEY", "test-secret")
    assert client.post("/chat", json={"message": "hi"}).status_code == 401
    assert (
        client.post(
            "/chat", json={"message": "hi"}, headers={"X-API-Key": "wrong"}
        ).status_code
        == 401
    )
    r = client.post(
        "/chat", json={"message": "hi"}, headers={"X-API-Key": "test-secret"}
    )
    assert r.status_code == 200
    # Health check stays open for uptime monitors
    assert client.get("/health").status_code == 200


def test_reminder_accepts_friendly_times(client):
    # User feedback 2026-11-11: typing "6:30 p.m." got a 422. Never again.
    r = client.post("/reminders", json={"text": "meds", "due_at": "6 pm"})
    assert r.status_code == 201 and r.json()["due_at"].endswith("T18:00")
    r = client.post("/reminders", json={"text": "meds", "due_at": "6:30 p.m."})
    assert r.status_code == 201 and r.json()["due_at"].endswith("T18:30")
    r = client.post("/reminders", json={"text": "stretch", "due_at": "in 30 minutes"})
    assert r.status_code == 201
    r = client.post("/reminders", json={"text": "bins", "due_at": "tomorrow at 9 am"})
    assert r.status_code == 201 and r.json()["due_at"].endswith("T09:00")


def test_friendly_time_error_message_is_helpful(client):
    r = client.post("/reminders", json={"text": "x", "due_at": "blorp"})
    assert r.status_code == 422
    assert "6 pm" in r.json()["detail"]  # shows examples, not programmer-speak
