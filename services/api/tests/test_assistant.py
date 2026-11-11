"""Tests for the real assistant (user feedback 2026-11-10: stop the echo)."""

import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("BIH_DB_PATH", str(tmp_path / "test.db"))
    from app.main import app

    return TestClient(app)


def chat(client, message):
    r = client.post("/chat", json={"message": message})
    assert r.status_code == 200
    body = r.json()
    assert body["safety_flag"] is False
    return body["reply"]


def test_reminder_without_time_asks_when(client):
    reply = chat(client, "remind me to call the doctor")
    assert "When should I remind you" in reply
    # Nothing saved yet — confirm-before-save principle.
    assert client.get("/reminders").json() == []


def test_reminder_with_time_is_saved(client):
    reply = chat(client, "remind me to take meds at 6 pm")
    assert "Done" in reply
    reminders = client.get("/reminders").json()
    assert len(reminders) == 1
    assert reminders[0]["text"] == "take meds"
    assert reminders[0]["due_at"].endswith("T18:00")


def test_reminder_relative_time(client):
    reply = chat(client, "remind me to stretch in 30 minutes")
    assert "Done" in reply
    assert client.get("/reminders").json()[0]["text"] == "stretch"


def test_remember_to_also_sets_reminder(client):
    chat(client, "remember to drink water in 1 hour")
    assert client.get("/reminders").json()[0]["text"] == "drink water"


def test_whats_on_lists_reminders(client):
    chat(client, "remind me to call mum at 6 pm")
    reply = chat(client, "what's on today?")
    assert "call mum" in reply


def test_when_is_looks_up_reminder(client):
    chat(client, "remind me about the dentist at 9 am")
    reply = chat(client, "when is my dentist?")
    assert "9:00 am" in reply


def test_remember_that_saves_note(client):
    reply = chat(client, "remember that my red folder has my forms")
    assert "Saved" in reply
    assert client.get("/notes").json()[0]["text"] == "my red folder has my forms"


def test_read_my_notes(client):
    chat(client, "remember that parking is level 2")
    reply = chat(client, "read my notes")
    assert "parking is level 2" in reply


def test_routine_walkthrough(client):
    client.post("/routines", json={"name": "morning", "steps": ["Wake up", "Take meds"]})
    reply = chat(client, "morning routine")
    assert "1. Wake up" in reply and "2. Take meds" in reply


def test_fallback_helps_without_echoing(client):
    reply = chat(client, "flibberty gibbet")
    assert "reminder" in reply  # points at capabilities
    assert "You said" not in reply  # never a parrot again


def test_crisis_still_wins_over_assistant(client):
    # "help" is an assistant intent — but crisis detection runs first and
    # always wins.
    r = client.post("/chat", json={"message": "help me, I want to die"})
    assert r.json()["safety_flag"] is True
    assert "988" in r.json()["reply"]


def test_voice_uses_assistant(client):
    r = client.post("/voice", json={"transcript": "what time is my appointment"})
    assert r.status_code == 200
    assert "appointment" in r.json()["reply_text"]


def test_times_use_12_hour_clock(client):
    # User feedback 2026-11-11: 24-hour "military" time is hard to read.
    reply = chat(client, "remind me to take meds at 6 pm")
    assert "6:00 pm" in reply
    assert "18:00" not in reply
