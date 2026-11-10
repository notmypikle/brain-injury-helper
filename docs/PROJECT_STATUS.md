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

## User ideas log (things the user asked for — keep building on these)
- **2026-11-02 — Cross-app integration + e-signature** (user's words: needs
  it as a core need): view whatever is on screen, fill it with
  saved private details, e-sign. Built as: BihAccessibilityService
  (screen fill with confirmation, optional Send/Submit tap) + draw-with-
  finger signature pad (encrypted, on-device only) + typed e-signature in
  signature boxes. Risks S12/S13/I7 added to the risk map.
- **2026-11-02 — Online document auto-fill** (both wizard + direct fill):
  paperwork wizard (merged) + autofill service (merged).
- **2026-11-10 — Legal paperwork help** (a real need: "there's always
  legal paperwork when dealing with life as a human"): 4 letter templates
  added to the wizard — benefits appeal, formal complaint, records request
  (subject access), adjustments request. Hard rule: letters and
  information only, NEVER legal advice; every template says so and points
  to free qualified help (risk S14, enforced by a test).
- **2026-11-10 — Voice separation / "who said what"** (user saw an article
  on AI separating overlapping voices): future feature idea — conversation
  notes that transcribe WHO said WHAT (memory support after group
  conversations or appointments), possibly focus-one-voice listening.
  Now feasible on-device (Pyannote 4.0 diarization, on-device separation),
  which fits the privacy model. Phase 5+.
- **2026-11-10 — Interested in:** AI security review of the code (like
  GPT-6 Cyber) and on-device AI via LiteRT/Gemini Nano for real chat
  answers. Security review DONE (see below); on-device AI chat queued.

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
- **On-device encryption (S4 ✅):** the Android API key and cached profile
  are now encrypted with the Android Keystore (hardware-backed
  AES-256-GCM). S3 partially mitigated via Android full-disk encryption;
  SQLCipher remains the Phase 4 belt-and-braces step.

## Security review (2026-11-10 — user-requested)
Full codebase audit by a dedicated review agent. **2 findings + 1 defect,
all fixed same day:**
1. 🟡 Mic could send voice off-device → now prefers on-device recognition
   and warns the user when unavailable (risk P11).
2. ⚪ Screen-fill re-scanned after confirmation → now fills ONLY the exact
   boxes the user confirmed.
3. 🐛 Accessibility service crashed on Android 8–12 → manifest permission
   added.
Everything else checked clean (auth coverage, SQL, template injection,
Keystore encryption, exported components, DoS limits).

## Where we left off / next steps (in order)
1. **Merge the open PRs, install the APK from the Actions tab, and test on
   the phone:** autofill service (Settings → System → Autofill service →
   B.I.H.) and the new screen-fill service (Settings → Accessibility →
   B.I.H.; on Android 13+ first allow restricted settings — see risk I7).
2. **Security review of the API code** (user-approved interest,
   2026-11-10).
3. **On-device AI chat** via Gemini Nano/LiteRT (user-approved interest,
   2026-11-10) — replaces the placeholder chat reply with real on-device
   answers; private and free.
4. **E-signature follow-up:** stamp the drawn signature into exported
   documents when PDF export is added (Phase 4).
5. Then Phase 3 beta items in VOICE_AND_PRODUCTION.md.

## Session checklist (do this every session)
1. Read this file first.
2. Run the API tests: `cd services/api && pytest` (must stay green).
3. Do the next step(s) above.
4. Update this file + check off items in VOICE_AND_PRODUCTION.md.
5. Add any new risks discovered to RISK_AND_ISSUE_MAP.md.
