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

See `docs/IMPLEMENTATION_PLAN.md` and `docs/PRIVACY_AND_SAFETY.md`.
