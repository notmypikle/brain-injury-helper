# Project Status — B.I.H.

*Where the project stands and what's next. Update this file at the end of
every work session so the next session can pick up without re-discovery.*

Last updated: 2026-11-02

## Current state

| Area | Status | Notes |
|---|---|---|
| Phase 0 — scaffolding, health check, SQLite, privacy doc | ✅ Done | |
| Phase 1 — chat, notes, reminders, routines | ✅ Done | 12 tests passing |
| Phase 2 — Android app | ✅ Coded | All screens, voice, notifications, autofill; pending first build + on-device test |
| Phase 3 — voice, paperwork, export | 🔶 Mostly done | Voice (mic + TTS on Chat), paperwork wizard + export all built; needs on-device testing |
| Phase 4 — encryption at rest, export/delete, local LLM | 🔶 Partial | `GET /export` + `DELETE /data` done; encryption at rest + local LLM pending |

## What was done this session
- Created this status file.
- Scaffolded `apps/android`: Kotlin + Jetpack Compose project with Chat,
  Today, Notes, Routines, and Settings screens, plus an API client.
- **Paperwork wizard** (for forms that can only be filed online):
  - API: `/profile` (personal details stored once), `/paperwork/templates`
    (doctor visit, medication list, benefits/disability answers, emergency
    card), `/paperwork/fill` (auto-fills from profile, reports missing
    fields), document list/get/delete, and plain-text export.
  - Android: new **Forms** tab — My details editor, form picker, one-field-
    at-a-time fill with profile pre-fill, copy-friendly document view.
  - 21 API tests passing (9 new for paperwork).
- **Online auto-fill (part 2, user-requested):** Android Autofill service
  (`apps/android/.../autofill/`) that pushes saved My-details answers
  straight into web forms. Confirmation screen shows every field before
  anything is filled; passwords/usernames/cards are never touched; the
  service reads only the on-device profile cache (no network). Needs
  on-device testing — see next steps.
- **Voice on the Chat screen:** mic button (on-device SpeechRecognizer;
  transcript lands in the text box for review before sending — risk P5)
  and spoken replies via on-device TextToSpeech with a mute toggle.
- **Reminder time validation (risk P2):** API rejects non-ISO-8601
  `due_at` values with 422; 2 new tests (23 total passing).
- **Not yet verified:** the Android project has not been built (needs
  Android Studio / Android SDK, which this environment doesn't have).
  First build may need small fixes — treat that as expected, not a failure.

- **Reminder notifications:** the Today screen can now create reminders,
  and each one schedules a local notification at its due time
  (WorkManager; notification permission requested on Android 13+).
  Marking a reminder done cancels its notification.
- **Data rights (launch gate, risks S8/O1):** `GET /export` downloads all
  data as one JSON file; `DELETE /data` wipes everything. WAL mode
  enabled for crash safety (O2 ✅). 25 tests passing.

## Where we left off / next steps (in order)
1. **Open `apps/android` in Android Studio and build it.** Fix any
   first-build errors (risk I6 in RISK_AND_ISSUE_MAP.md). Phase 2 is now
   fully coded — this build check is the gate.
2. **Enable and test the new autofill service on a real device/emulator:**
   Settings → System → Autofill service → choose B.I.H. Then open an
   online form and check the confirmation screen appears (risk S11).
   Save My details in the Forms tab first — autofill reads that cache.
3. Then Phase 3 beta items in VOICE_AND_PRODUCTION.md.

## Session checklist (do this every session)
1. Read this file first.
2. Run the API tests: `cd services/api && pytest` (must stay green).
3. Do the next step(s) above.
4. Update this file + check off items in VOICE_AND_PRODUCTION.md.
5. Add any new risks discovered to RISK_AND_ISSUE_MAP.md.
