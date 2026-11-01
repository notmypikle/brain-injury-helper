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
- `apps/android`: Kotlin + Jetpack Compose app (planned)
- `docs`: plan and privacy model

## Run the API (Termux or computer)
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
