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
    # ------------------------------------------------------------------
    # USER'S IDEA (Nov 2026): "legal paperwork — there's always legal
    # paperwork when dealing with life as a human."
    # These are LETTERS, not legal advice (risk S14): every description
    # says so and points to free qualified help. B.I.H. never tells the
    # user what the law says about their case — it helps them get their
    # situation onto paper, one calm question at a time.
    # ------------------------------------------------------------------
    {
        "id": "benefits-appeal-letter",
        "title": "Benefits decision appeal letter",
        "description": (
            "Challenge a benefits decision you disagree with. This is a "
            "letter template, not legal advice — free help: Citizens Advice "
            "or a welfare rights adviser."
        ),
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "dob", "label": "Date of birth", "auto": True},
            {"key": "address", "label": "Home address", "auto": True},
            {"key": "phone", "label": "Phone number", "auto": True},
            {"key": "email", "label": "Email address", "auto": True},
            {"key": "reference", "label": "Reference number on the decision letter", "auto": False},
            {"key": "decision_date", "label": "Date on the decision letter", "auto": False},
            {"key": "what_disagree", "label": "What decision are you challenging?", "auto": False},
            {"key": "why_disagree", "label": "Why is it wrong? (in your own words)", "auto": False},
        ],
        "body": (
            "{full_name}\n{address}\n{phone}\n{email}\n"
            "\nDate of birth: {dob}\nReference: {reference}\n"
            "\nTo whom it may concern,\n"
            "\nI am writing to challenge the decision dated {decision_date} "
            "(reference {reference}).\n"
            "\nDecision I am challenging:\n{what_disagree}\n"
            "\nWhy I believe the decision is wrong:\n{why_disagree}\n"
            "\nPlease reconsider this decision and send me a written response.\n"
            "\nYours faithfully,\n{full_name}\n"
        ),
    },
    {
        "id": "complaint-letter",
        "title": "Formal complaint letter",
        "description": (
            "Complain to a company or service and say what you want done. "
            "A letter template, not legal advice."
        ),
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "address", "label": "Home address", "auto": True},
            {"key": "email", "label": "Email address", "auto": True},
            {"key": "company", "label": "Who are you complaining to?", "auto": False},
            {"key": "what_happened", "label": "What happened?", "auto": False},
            {"key": "when_happened", "label": "When did it happen?", "auto": False},
            {"key": "what_you_want", "label": "What do you want them to do?", "auto": False},
        ],
        "body": (
            "{full_name}\n{address}\n{email}\n"
            "\nTo: {company}\n"
            "\nDear Sir or Madam,\n"
            "\nI am writing to make a formal complaint.\n"
            "\nWhat happened:\n{what_happened}\n"
            "\nWhen it happened: {when_happened}\n"
            "\nWhat I would like you to do:\n{what_you_want}\n"
            "\nPlease respond in writing within 14 days.\n"
            "\nYours faithfully,\n{full_name}\n"
        ),
    },
    {
        "id": "records-request-letter",
        "title": "Request for my records",
        "description": (
            "Ask an organisation for a copy of the records they hold about "
            "you (a 'subject access request'). A letter template, not "
            "legal advice."
        ),
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "dob", "label": "Date of birth", "auto": True},
            {"key": "address", "label": "Home address", "auto": True},
            {"key": "email", "label": "Email address", "auto": True},
            {"key": "organisation", "label": "Which organisation? (e.g. hospital, council)", "auto": False},
            {"key": "records_wanted", "label": "Which records? (e.g. all medical notes)", "auto": False},
            {"key": "date_range", "label": "Dates covering (e.g. 2024 to now)", "auto": False},
        ],
        "body": (
            "{full_name}\n{address}\n{email}\n"
            "\nTo: {organisation}\n"
            "\nDear Sir or Madam,\n"
            "\nI am requesting a copy of the personal data you hold about me. "
            "This is a subject access request.\n"
            "\nMy details:\nName: {full_name}\nDate of birth: {dob}\nAddress: {address}\n"
            "\nRecords requested: {records_wanted}\nDates covering: {date_range}\n"
            "\nPlease send the records to my address or email above.\n"
            "\nYours faithfully,\n{full_name}\n"
        ),
    },
    {
        "id": "adjustments-request-letter",
        "title": "Request for adjustments (work, housing, services)",
        "description": (
            "Ask an employer, landlord, or service for changes that make "
            "things accessible for you. A letter template, not legal "
            "advice — free help: Citizens Advice or a disability charity."
        ),
        "fields": [
            {"key": "full_name", "label": "Your full name", "auto": True},
            {"key": "address", "label": "Home address", "auto": True},
            {"key": "email", "label": "Email address", "auto": True},
            {"key": "recipient", "label": "Who is this to? (e.g. manager, landlord)", "auto": False},
            {"key": "my_needs", "label": "What do you find difficult?", "auto": False},
            {"key": "adjustments", "label": "What changes would help?", "auto": False},
        ],
        "body": (
            "{full_name}\n{address}\n{email}\n"
            "\nTo: {recipient}\n"
            "\nDear Sir or Madam,\n"
            "\nI am writing to request adjustments so that I can manage "
            "day to day.\n"
            "\nWhat I find difficult:\n{my_needs}\n"
            "\nChanges that would help me:\n{adjustments}\n"
            "\nI am happy to discuss this at a time that suits you. "
            "Please respond in writing.\n"
            "\nYours faithfully,\n{full_name}\n"
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


# --- Checklists (user request 2026-11-11): "B.I.H. needs to know what
# forms are which and what is required on them, and have an area for
# verification paperwork so everything is in one place." -------------
# These tell the user WHICH form to ask an organization for and WHAT to
# gather first. Generic by design — no legal advice (rule S14).

CHECKLISTS: list[dict] = [
    {
        "id": "benefits-application",
        "title": "Benefits / disability application",
        "forms": ["The main benefit application (the office or its website names it)"],
        "gathers": [
            "Photo ID",
            "Social Security / national insurance number",
            "Medical records or a doctor's letter",
            "Diagnosis / treatment history",
            "Work history",
            "Bank details (for payments)",
            "Housing costs (rent statement or lease)",
        ],
        "note": "Save copies of everything in the Verification documents folder before submitting.",
    },
    {
        "id": "benefits-appeal",
        "title": "Benefits appeal",
        "forms": ["The appeal form named in the decision letter"],
        "gathers": [
            "The decision letter itself",
            "Any deadlines written in it (mark them as reminders!)",
            "Medical evidence",
            "Your dated incident/symptom notes",
            "Statements from people who know your situation",
        ],
        "note": "Deadlines matter most — set a reminder the day the letter arrives.",
    },
    {
        "id": "housing-support",
        "title": "Housing / rent assistance",
        "forms": ["The local housing program's application (the housing office names it)"],
        "gathers": [
            "Lease or rental agreement",
            "Ledger of what you owe",
            "Income proof",
            "Any eviction or notice paperwork",
            "Photo ID",
        ],
        "note": "Keep every notice you receive — dated paperwork wins cases.",
    },
    {
        "id": "medical-records-request",
        "title": "Medical records request",
        "forms": ["The provider's records-release form (their office or patient portal has it)"],
        "gathers": [
            "Photo ID",
            "Dates of treatment (approximate is fine)",
            "Which records you need (visits, scans, notes)",
            "Where records should be sent",
        ],
        "note": "You have a legal right to your own records. If refused, ask for the refusal in writing.",
    },
    {
        "id": "identity-verification",
        "title": "Identity verification",
        "forms": ["The requesting organization's own form"],
        "gathers": [
            "Photo ID",
            "Proof of address (utility bill, bank statement)",
            "Birth certificate or Social Security card (only when required)",
        ],
        "note": "Share identity documents only through the organization's official site — check the web address carefully.",
    },
]


def get_checklist(checklist_id: str) -> dict | None:
    for c in CHECKLISTS:
        if c["id"] == checklist_id:
            return c
    return None



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
