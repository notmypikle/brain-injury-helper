import pytest
from fastapi.testclient import TestClient


@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("BIH_DB_PATH", str(tmp_path / "test.db"))
    from app.main import app

    return TestClient(app)


def test_profile_starts_empty(client):
    assert client.get("/profile").json() == {}


def test_profile_set_get_delete(client):
    r = client.put("/profile", json={"fields": {"full_name": "Sam Lee", "dob": "1980-05-04"}})
    assert r.status_code == 200
    assert r.json() == {"dob": "1980-05-04", "full_name": "Sam Lee"}

    # Upsert overwrites just one key.
    client.put("/profile", json={"fields": {"dob": "1980-05-05"}})
    assert client.get("/profile").json()["dob"] == "1980-05-05"

    assert client.delete("/profile/dob").status_code == 204
    assert "dob" not in client.get("/profile").json()
    assert client.delete("/profile/dob").status_code == 404


def test_profile_rejects_bad_key_and_long_value(client):
    r = client.put("/profile", json={"fields": {"Bad Key!": "x"}})
    assert r.status_code == 422
    r = client.put("/profile", json={"fields": {"ok": "x" * 2001}})
    assert r.status_code == 422


def test_templates_listed(client):
    r = client.get("/paperwork/templates")
    assert r.status_code == 200
    ids = [t["id"] for t in r.json()]
    assert "doctor-visit" in ids
    assert "benefits-form" in ids
    assert all(t["fields"] for t in r.json())


def test_legal_letter_templates_listed(client):
    """User-requested legal paperwork templates (Nov 2026)."""
    ids = [t["id"] for t in client.get("/paperwork/templates").json()]
    for expected in (
        "benefits-appeal-letter",
        "complaint-letter",
        "records-request-letter",
        "adjustments-request-letter",
    ):
        assert expected in ids


def test_legal_templates_carry_no_advice_disclaimer(client):
    """Every legal template must say it is not legal advice (risk S14)."""
    templates = client.get("/paperwork/templates").json()
    legal = [t for t in templates if t["id"].endswith("-letter")]
    assert len(legal) >= 4
    for t in legal:
        assert "not legal advice" in t["description"].lower()


def test_benefits_appeal_letter_fills_from_profile(client):
    client.put("/profile", json={"fields": {"full_name": "Sam Lee", "address": "1 High St"}})
    r = client.post("/paperwork/fill", json={
        "template_id": "benefits-appeal-letter",
        "values": {"what_disagree": "They refused my claim"},
    })
    assert r.status_code == 201
    body = r.json()
    assert body["content"].startswith("Sam Lee\n1 High St")
    assert "They refused my claim" in body["content"]
    assert "reference" in body["missing_fields"]  # still needs the letter's ref number


def test_fill_autofills_from_profile_and_reports_missing(client):
    client.put("/profile", json={"fields": {"full_name": "Sam Lee", "dob": "1980-05-04"}})
    r = client.post("/paperwork/fill", json={
        "template_id": "doctor-visit",
        "values": {"reason": "Headaches", "visit_date": "2026-11-10"},
    })
    assert r.status_code == 201
    body = r.json()
    assert "Name: Sam Lee" in body["content"]          # auto-filled from profile
    assert "Reason for visit:\nHeadaches" in body["content"]
    assert "symptoms" in body["missing_fields"]         # not provided anywhere
    assert "full_name" not in body["missing_fields"]


def test_fill_explicit_values_beat_profile(client):
    client.put("/profile", json={"fields": {"full_name": "Sam Lee"}})
    r = client.post("/paperwork/fill", json={
        "template_id": "doctor-visit",
        "values": {"full_name": "Sam A. Lee"},
    })
    assert "Name: Sam A. Lee" in r.json()["content"]


def test_fill_unknown_template_404(client):
    r = client.post("/paperwork/fill", json={"template_id": "nope", "values": {}})
    assert r.status_code == 404


def test_documents_list_get_delete(client):
    r = client.post("/paperwork/fill", json={"template_id": "emergency-card", "values": {}})
    doc_id = r.json()["id"]

    docs = client.get("/paperwork/documents").json()
    assert [d["id"] for d in docs] == [doc_id]

    one = client.get(f"/paperwork/documents/{doc_id}")
    assert one.status_code == 200
    assert one.json()["title"] == "Emergency information card"

    assert client.delete(f"/paperwork/documents/{doc_id}").status_code == 204
    assert client.get(f"/paperwork/documents/{doc_id}").status_code == 404
    assert client.delete(f"/paperwork/documents/{doc_id}").status_code == 404


def test_export_downloads_text(client):
    r = client.post("/paperwork/fill", json={
        "template_id": "medication-list",
        "values": {"full_name": "Sam Lee"},
    })
    doc_id = r.json()["id"]
    r = client.get(f"/paperwork/documents/{doc_id}/export")
    assert r.status_code == 200
    assert "attachment" in r.headers["content-disposition"]
    assert "Name: Sam Lee" in r.text
