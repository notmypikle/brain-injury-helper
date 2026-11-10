# B.I.H. — Brain Injury Helper

Privacy-first voice + text assistant for daily support, built for people with brain injuries.

> **Important:** B.I.H. is not a medical provider and does not replace professional care.

## Prototype scope (v0.1-prototype-android-single-user)
- Android only, single user, private repo
- Chat (text first, then voice)
- Memory journal, reminders, routines
- Paperwork helper
- Safety / emergency contacts

## Stack
- `services/api`: Python FastAPI backend
- `apps/cli`: Python command-line client
- `apps/android`: Kotlin + Jetpack Compose app (scaffolded — see its README)
- `docs`: plan and privacy model

## Run the API (Termux or computer)
Requires **Python 3.10 or newer** (`python --version` to check).
```bash
cd services/api
pip install -r requirements.txt
uvicorn app.main:app --port 8000
```

## Run the CLI
```bash
cd apps/cli
pip install -r requirements.txt
python main.py
```

Set `BIH_API_URL` to point the CLI at a different API address.

## API endpoints
- `GET /health` — liveness check (always open)
- `POST /chat` — chat reply; detects possible crisis language and responds
  with crisis guidance plus a `safety_flag` (see `docs/PRIVACY_AND_SAFETY.md`)
- `POST /voice` — single round-trip for voice clients: transcript in,
  reply text out (device handles speech-to-text and text-to-speech
  on-device; see `docs/VOICE_AND_PRODUCTION.md`)
- `POST /notes` / `GET /notes` / `DELETE /notes/{id}` — memory journal
- `POST /reminders` / `GET /reminders` / `POST /reminders/{id}/done` /
  `DELETE /reminders/{id}` — reminders with due times
- `POST /routines` / `GET /routines` / `DELETE /routines/{id}` — step-by-step
  routine checklists
- `GET /profile` / `PUT /profile` / `DELETE /profile/{key}` — personal details
  stored once and used to auto-fill paperwork
- `GET /paperwork/templates` — built-in form templates (doctor visit summary,
  medication list, benefits/disability answers, emergency card)
- `POST /paperwork/fill` — fill a template (auto-fills from profile, reports
  what's still missing) and save the finished document
- `GET /paperwork/documents` / `GET|DELETE /paperwork/documents/{id}` /
  `GET /paperwork/documents/{id}/export` — manage and download finished
  documents (plain-text export for copying into online forms)

- `GET /export` — download ALL your data as one JSON file (backup / portability)
- `DELETE /data` — permanently delete everything (notes, reminders, routines,
  profile, documents). No undo.

For forms that can only be filed online, the Android app also provides an
**autofill service**: after saving your details in the Forms tab, enable
B.I.H. under Settings → System → Autofill service, and it offers to fill
matching web-form fields — always with a confirmation screen first, and
never passwords, usernames, or card details.

It also provides an **accessibility service** for cross-app help (works in
any app, not just browsers): tap the accessibility button on any screen and
B.I.H. offers to fill recognised empty boxes with your saved details, and
can tap Send/Submit — always after your explicit confirmation. Signature
boxes receive your typed full name (a legally recognised e-signature), and
a drawn signature can be saved encrypted on-device in the Forms tab.

All data is stored locally in SQLite (`bih.db` by default; override with
`BIH_DB_PATH`). Set `BIH_API_KEY` to require an `X-API-Key` header on every
endpoint except `/health`.

## Docs
- `docs/VOICE_AND_PRODUCTION.md` — voice architecture + concept→production roadmap
- `docs/MONETIZATION.md` — ethical pricing tiers and revenue plan
- `docs/RISK_AND_ISSUE_MAP.md` — every known/theoretical issue + resolutions
- `docs/RISK_MAP_TEMPLATE.md` — reusable risk template for other projects
- `docs/IMPLEMENTATION_PLAN.md`, `docs/PRIVACY_AND_SAFETY.md`

## Run the tests
```bash
cd services/api
pip install -r requirements-dev.txt
pytest
```

See `docs/IMPLEMENTATION_PLAN.md` and `docs/PRIVACY_AND_SAFETY.md`.
