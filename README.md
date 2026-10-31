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

Android status:
- Basic app skeleton now exists in `apps/android`
- Includes biometric unlock + backup 6-digit PIN flow
- Includes chat + paperwork API calls and thought-rescue tools (hold/restore/auto-save/talk)
- Includes call support mode (script builder + open dialer with number)

## Run the API (Termux or computer)
```bash
cd services/api
pip install -r requirements.txt
export OPENAI_API_KEY="your-key-here"
# optional: export OPENAI_MODEL="gpt-4o-mini"
uvicorn app.main:app --port 8000
```

## Run the CLI
```bash
cd apps/cli
pip install -r requirements.txt
python main.py
```

CLI tips:
- Normal message = chat
- `/paperwork` = paste paperwork text and get simple actions/deadlines/questions
- `exit` = quit

See `docs/IMPLEMENTATION_PLAN.md` and `docs/PRIVACY_AND_SAFETY.md`.

## Paperwork Helper API (prototype)
Paste paperwork text and get a simple breakdown:

```bash
curl -X POST http://127.0.0.1:8000/paperwork/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "text": "Please submit proof of income by November 20, 2026. Call 555-0100 if you need help."
  }'
```

Response fields:
- `plain_summary`
- `key_actions`
- `deadlines`
- `questions_to_ask`
- `safety_note`

If `OPENAI_API_KEY` is missing, `/chat` and `/paperwork/analyze` fail closed with an error.
