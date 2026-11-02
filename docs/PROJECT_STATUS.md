# Project Status — B.I.H. (session handoff file)

*Read this first when resuming work in a new session. Update it in every PR.*
*Last updated: 2026-11-02*

## Where we are
- **Phase 1 (backend): DONE ✅** — chat + crisis detection, notes, reminders,
  routines, `/voice`, optional API-key auth, 12 tests passing
- **Phase 2 (Android app): IN PROGRESS 🔨** — full Kotlin + Jetpack Compose
  scaffold built: Chat (text + 🎙️ voice), Today, Notes, Settings screens,
  Retrofit API client, DataStore settings, on-device STT/TTS.
  **Not yet compiled/run on a device** — needs Android Studio
  (see `apps/android/README.md` for the exact steps).

## Repos & workflow
- Repo: github.com/notmypikle/brain-injury-helper (public)
- All changes go through feature branch → PR → user merges in browser
- Agent tokens cannot push to `main` directly (by design)
- Python test env: `python3 -m venv`, install `services/api/requirements-dev.txt`,
  run `pytest` from repo root
- Android app: open `apps/android/` in Android Studio; it downloads the
  Gradle wrapper + SDK on first sync

## Next tasks (in order)
1. First on-device build of the Android app (needs Android Studio + phone/emulator)
2. Local notifications for reminders
3. App lock (biometric/PIN)
4. Then Phase 3 beta: accessibility audit, closed beta testing

## Key docs
- `docs/VOICE_AND_PRODUCTION.md` — full roadmap with launch gates
- `docs/MONETIZATION.md` — pricing tiers (Free / Plus $4.99 / Caregiver $9.99)
- `docs/RISK_AND_ISSUE_MAP.md` — 30 tracked issues; update in every PR
- `docs/IMPLEMENTATION_PLAN.md`, `docs/PRIVACY_AND_SAFETY.md`

## Conventions
- Python 3.10+, pinned deps, `BIH_DB_PATH` / `BIH_API_KEY` / `BIH_API_URL` env vars
- Low cognitive load UI: big text, one action per screen, always confirm saves
- Voice is always on-device (SpeechRecognizer/TTS); text input always available
- Never log user message content (privacy rule S6)
