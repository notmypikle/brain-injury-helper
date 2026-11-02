import json

import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("BIH_DB_PATH", str(tmp_path / "test.db"))
    from app.main import app

    return TestClient(app)


def _seed(client):
    client.post("/notes", json={"text": "Remember this"})
    client.post("/reminders", json={"text": "Take meds", "due_at": "2026-11-03T09:00"})
    client.post("/routines", json={"name": "Morning", "steps": ["Wake", "Meds"]})
    client.put("/profile", json={"fields": {"full_name": "Sam Lee"}})
    client.post("/paperwork/fill", json={"template_id": "doctor-visit", "values": {}})


def test_export_returns_everything(client):
    _seed(client)
    r = client.get("/export")
    assert r.status_code == 200
    assert "attachment" in r.headers["content-disposition"]
    data = json.loads(r.text)
    assert data["notes"][0]["text"] == "Remember this"
    assert data["reminders"][0]["text"] == "Take meds"
    assert data["routines"][0]["steps"] == ["Wake", "Meds"]
    assert data["profile"]["full_name"] == "Sam Lee"
    assert len(data["documents"]) == 1


def test_delete_all_data_wipes_everything(client):
    _seed(client)
    r = client.delete("/data")
    assert r.status_code == 200
    assert r.json() == {"deleted": True}

    assert client.get("/notes").json() == []
    assert client.get("/reminders?include_done=true").json() == []
    assert client.get("/routines").json() == []
    assert client.get("/profile").json() == {}
    assert client.get("/paperwork/documents").json() == []
