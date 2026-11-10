"""Tests for calm mode (overstimulation support, user need 2026-11-10)."""

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
    return r.json()


def test_calm_steps_endpoint(client):
    r = client.get("/calm/steps")
    assert r.status_code == 200
    steps = r.json()["steps"]
    assert len(steps) >= 5


def test_steps_are_short_and_single_action(client):
    # Low cognitive load: every step must be short and one instruction.
    for step in client.get("/calm/steps").json()["steps"]:
        assert len(step) <= 90, step
        assert step.count(".") <= 2, step


def test_too_much_gets_calm_reply(client):
    body = chat(client, "it's all too much today")
    assert body["safety_flag"] is False
    assert "Too much" in body["reply"]  # points at the calm-mode button
    assert "breathe" in body["reply"].lower()  # starts calming right away


def test_overload_variations(client):
    for phrase in ("I'm overwhelmed", "it's too loud in here",
                   "I can't cope", "everything is too busy"):
        assert chat(client, phrase)["safety_flag"] is False
        assert "breathe" in chat(client, phrase)["reply"].lower()


def test_crisis_still_wins_over_overload(client):
    # Calm mode must never mask a crisis: detection in main.py runs first.
    body = chat(client, "it's too much, I want to die")
    assert body["safety_flag"] is True
    assert "988" in body["reply"]


def test_voice_overload_uses_calm_reply(client):
    r = client.post("/voice", json={"transcript": "it's too much"})
    assert r.status_code == 200
    assert "breathe" in r.json()["reply_text"].lower()
