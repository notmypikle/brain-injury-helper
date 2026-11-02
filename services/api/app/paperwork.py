"""Paperwork wizard: built-in form templates + auto-fill from the profile.

Designed for forms that can only be filed online: the app collects answers
one step at a time (low cognitive load), auto-fills anything already known
from the user's profile, and produces a finished document whose lines are
easy to copy one at a time into the web form.

Fields marked auto=True are filled from the profile automatically when the
profile has a value under the same key. Missing fields render as blanks so
the user can see exactly what is still needed.
"""

# Standard profile keys (also used by the Android "My details" editor):
# full_name, dob, address, phone, email, doctor_name, doctor_phone,
# emergency_contact_name, emergency_contact_phone, allergies, conditions,
# medications

TEMPLATES: list[dict] = [
    {
        "id": "doctor-visit",
        "title": "Doctor visit summary",
        "description": "Bring this to an appointment so nothing is forgotten.",
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "dob", "label": "Date of birth", "auto": True},
            {"key": "visit_date", "label": "Date of the visit", "auto": False},
            {"key": "doctor_name", "label": "Doctor's name", "auto": True},
            {"key": "reason", "label": "Reason for the visit", "auto": False},
            {"key": "symptoms", "label": "Symptoms since last visit", "auto": False},
            {"key": "questions", "label": "Questions to ask", "auto": False},
        ],
        "body": (
            "DOCTOR VISIT SUMMARY\n"
            "====================\n"
            "Name: {full_name}\n"
            "Date of birth: {dob}\n"
            "Visit date: {visit_date}\n"
            "Doctor: {doctor_name}\n"
            "\nReason for visit:\n{reason}\n"
            "\nSymptoms since last visit:\n{symptoms}\n"
            "\nQuestions to ask:\n{questions}\n"
        ),
    },
    {
        "id": "medication-list",
        "title": "Medication list",
        "description": "An up-to-date list of medicines to hand over or copy from.",
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "dob", "label": "Date of birth", "auto": True},
            {"key": "allergies", "label": "Allergies", "auto": True},
            {"key": "medications", "label": "Medications (one per line: name, dose, when)", "auto": True},
            {"key": "doctor_name", "label": "Doctor's name", "auto": True},
            {"key": "doctor_phone", "label": "Doctor's phone", "auto": True},
        ],
        "body": (
            "MEDICATION LIST\n"
            "===============\n"
            "Name: {full_name}\n"
            "Date of birth: {dob}\n"
            "Allergies: {allergies}\n"
            "\nMedications:\n{medications}\n"
            "\nDoctor: {doctor_name}\n"
            "Doctor's phone: {doctor_phone}\n"
        ),
    },
    {
        "id": "benefits-form",
        "title": "Benefits / disability application answers",
        "description": (
            "For online benefits or disability forms: collect the answers "
            "here, then copy each one into the web form, one box at a time."
        ),
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "dob", "label": "Date of birth", "auto": True},
            {"key": "address", "label": "Home address", "auto": True},
            {"key": "phone", "label": "Phone number", "auto": True},
            {"key": "email", "label": "Email address", "auto": True},
            {"key": "condition_description", "label": "Your condition, in your own words", "auto": False},
            {"key": "daily_challenges", "label": "What is harder day to day because of it", "auto": False},
            {"key": "work_impact", "label": "How it affects work or study", "auto": False},
        ],
        "body": (
            "BENEFITS / DISABILITY APPLICATION — MY ANSWERS\n"
            "==============================================\n"
            "Copy each line below into the matching box of the online form.\n"
            "\nFull name: {full_name}\n"
            "Date of birth: {dob}\n"
            "Address: {address}\n"
            "Phone: {phone}\n"
            "Email: {email}\n"
            "\nAbout my condition:\n{condition_description}\n"
            "\nDay-to-day difficulties:\n{daily_challenges}\n"
            "\nEffect on work or study:\n{work_impact}\n"
        ),
    },
    {
        "id": "emergency-card",
        "title": "Emergency information card",
        "description": "Key facts for paramedics or hospital staff, all on one page.",
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "dob", "label": "Date of birth", "auto": True},
            {"key": "blood_type", "label": "Blood type (if known)", "auto": False},
            {"key": "conditions", "label": "Medical conditions", "auto": True},
            {"key": "allergies", "label": "Allergies", "auto": True},
            {"key": "medications", "label": "Current medications", "auto": True},
            {"key": "emergency_contact_name", "label": "Emergency contact name", "auto": True},
            {"key": "emergency_contact_phone", "label": "Emergency contact phone", "auto": True},
            {"key": "doctor_name", "label": "Doctor's name", "auto": True},
            {"key": "doctor_phone", "label": "Doctor's phone", "auto": True},
        ],
        "body": (
            "EMERGENCY INFORMATION\n"
            "=====================\n"
            "Name: {full_name}\n"
            "Date of birth: {dob}\n"
            "Blood type: {blood_type}\n"
            "\nMedical conditions: {conditions}\n"
            "Allergies: {allergies}\n"
            "Current medications: {medications}\n"
            "\nEMERGENCY CONTACT: {emergency_contact_name} — {emergency_contact_phone}\n"
            "Doctor: {doctor_name} — {doctor_phone}\n"
        ),
    },
]

BLANK = "____________________"


def get_template(template_id: str) -> dict | None:
    for t in TEMPLATES:
        if t["id"] == template_id:
            return t
    return None


def render(template: dict, profile: dict, values: dict) -> tuple[str, list[str]]:
    """Fill a template. values win over the profile. Returns (content,
    missing_keys) where missing_keys are fields with no value anywhere."""
    merged: dict[str, str] = {}
    missing: list[str] = []
    for field in template["fields"]:
        key = field["key"]
        value = (values.get(key) or "").strip()
        if not value and field["auto"]:
            value = (profile.get(key) or "").strip()
        if value:
            merged[key] = value
        else:
            merged[key] = BLANK
            missing.append(key)
    return template["body"].format(**merged), missing
