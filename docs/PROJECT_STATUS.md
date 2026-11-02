# Project Status — B.I.H.

*Where the project stands and what's next. Update this file at the end of
every work session so the next session can pick up without re-discovery.*

Last updated: 2026-11-02

## Current state

| Area | Status | Notes |
|---|---|---|
| Phase 0 — scaffolding, health check, SQLite, privacy doc | ✅ Done | |
| Phase 1 — chat, notes, reminders, routines | ✅ Done | 12 tests passing |
| Phase 2 — Android app | 🔨 In progress | Scaffolded; screens wired to API (see below) |
| Phase 3 — voice, paperwork, export | 🔶 Partial | `/voice` endpoint done; paperwork wizard + export not started |
| Phase 4 — encryption at rest, export/delete, local LLM | 📋 Planned | Launch gates, see VOICE_AND_PRODUCTION.md |

## What was done this session
- Created this status file.
- Scaffolded `apps/android`: Kotlin + Jetpack Compose project with Chat,
  Today, Notes, Routines, and Settings screens, plus an API client.
- **Not yet verified:** the Android project has not been built (needs
  Android Studio / Android SDK, which this environment doesn't have).
  First build may need small fixes — treat that as expected, not a failure.

## Where we left off / next steps (in order)
1. **Open `apps/android` in Android Studio and build it.** Fix any
   first-build errors (risk I6 in RISK_AND_ISSUE_MAP.md).
2. Add the mic button + TextToSpeech reply on the Chat screen
   (voice per VOICE_AND_PRODUCTION.md Part 1).
3. Local notifications for reminders (Android `WorkManager`/`AlarmManager`).
4. Reminder timezone validation (risk P2) — Android sends local time + offset.
5. Then Phase 3 beta items in VOICE_AND_PRODUCTION.md.

## Session checklist (do this every session)
1. Read this file first.
2. Run the API tests: `cd services/api && pytest` (must stay green).
3. Do the next step(s) above.
4. Update this file + check off items in VOICE_AND_PRODUCTION.md.
5. Add any new risks discovered to RISK_AND_ISSUE_MAP.md.
