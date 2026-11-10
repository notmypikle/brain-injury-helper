# Project Status — B.I.H.

*Where the project stands and what's next. Update this file at the end of
every work session so the next session can pick up without re-discovery.*

Last updated: 2026-11-10

## 🏆 Milestone: RUNNING ON THE USER'S PHONE (2026-11-10)
The full stack is live on-device: app (APK from CI, via the `apk` branch
direct-download) + API in Termux (F-Droid build) + Termux:Boot auto-start
(boot script at ~/.termux/boot/start-bih.sh, starts uvicorn ~30–60s after
boot; both Termux and Termux:Boot set to Battery → Unrestricted).
Chat + spoken replies confirmed working end-to-end by the user.
Gotchas encountered (all fixed/documented in docs/PHONE_SETUP.md):
VPN blocking loopback (test with VPN off), sideload signature match
(Termux F-Droid ↔ Termux:Boot F-Droid), stray text once corrupted the
phone's main.py (fixed via git reset --hard origin/main).

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
  on AI separating overlapping voices; real-world feedback: noise + too much
  simultaneous stimulation is a genuine daily barrier — treat as a CORE
  accessibility need, not a nice-to-have): future features — conversation
  notes that transcribe WHO said WHAT (memory support after group
  conversations or appointments), focus-one-voice listening, and possibly
  an in-the-moment "overload" helper (calm, one-step guidance when things
  get too loud/busy). Now feasible on-device (Pyannote 4.0 diarization,
  on-device separation), which fits the privacy model. Phase 5+.
  → FIRST PIECE BUILT 2026-11-10: the "Too much" calm-mode button (PR #19).
- **2026-11-10 — "Hey B.I.H." wake word** (like Gemini/Bixby): a true
  always-listening wake word is not available to third-party apps
  (phone-maker privilege) and would mean an always-on mic — against the
  privacy model. Built the closest equivalents instead: B.I.H. can be set
  as the DEFAULT ASSISTANT (hold-home / corner-swipe opens it from
  anywhere, even closed) + a Quick Settings tile. If Android ever opens
  wake words to third parties, revisit.
- **2026-11-10 — "B.I.H. only repeats what I ask it — fix that"**: replaced
  the placeholder echo with a real LOCAL assistant (app/assistant.py):
  sets reminders with time parsing ("at 6 pm", "in 30 minutes",
  "tomorrow"), saves notes, lists the day, looks up saved items, walks
  through routines, greets/thanks/help — all on-device, no LLM, crisis
  detection still runs first. 12 new tests (40 total).
- **2026-11-10 — First real-use bug report** (user testing!): chat wiped
  on tab switch + blank page on open → fixed (app-level ChatState +
  welcome message), then hardened to persist chat encrypted on-device so
  it survives app restarts too.
- **2026-11-10 — Interested in:** AI security review of the code (like
  GPT-6 Cyber) and on-device AI via LiteRT/Gemini Nano for real chat
  answers. Security review DONE (see below); on-device AI chat queued.

## What was done this session
- **"Too much" calm mode** (overstimulation support, core accessibility
  need): `GET /calm/steps` + assistant overload intent ("it's too much",
  "overwhelmed", "too loud", "can't cope" → starts calming immediately,
  points at the button; crisis detection still wins, test-enforced).
  Android: new Calm screen (one big-text step at a time, read-aloud OFF
  by default, bundled fallback steps so it works even with the API down)
  + always-visible "Too much? Tap here" button on Chat and Today.
  PR #19 open, CI build green; 46 API tests passing (6 new).
- **Published B.I.H. v0.1.3 to the `apk` branch** (built from CI run
  42922012521, PR #18 merge): real local assistant + hardened chat
  persistence. The direct download now has the assistant build.
- Session start: read this file, ran the tests (40 passing at the time),
  answered the user's APK install question ("app not found" — likely
  artifact-zip or per-build debug signature; see risk I8).

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
1. ✅ DONE 2026-11-10: app installed on the user's phone, API running in
   Termux, auto-start on boot working, chat + voice verified end-to-end.
2. **On-device feature tests** (user, at their own pace): autofill service
   (Settings → System → Autofill service → B.I.H.), screen-fill service
   (Settings → Accessibility → B.I.H.; Android 13+ restricted settings —
   risk I7), reminder notifications, Forms wizard, **calm mode** (once
   PR #19 is merged + installed: "Too much" button, and saying "it's too
   much" to the assistant).
3. **Merge PR #19 (calm mode)** then publish v0.1.4 to the `apk` branch
   from that CI build.
4. **Overstimulation support** (user's core need, 2026-11-10): ✅ "too
   much" calm-mode button coded (PR #19, CI green; needs merge + on-device
   test). Later: who-said-what / focus-one-voice.
5. **On-device AI chat, part 2** (user-approved interest, 2026-11-10):
   part 1 DONE — local assistant with real intents (app/assistant.py).
   Part 2: layer Gemini Nano/LiteRT free-form conversation on top, with
   the assistant module as the tool-use/fallback layer.
6. **E-signature follow-up:** stamp the drawn signature into exported
   documents when PDF export is added (Phase 4).
7. Then Phase 3 beta items in VOICE_AND_PRODUCTION.md.

## Session checklist (do this every session)
1. Read this file first.
2. Run the API tests: `cd services/api && pytest` (must stay green).
3. Do the next step(s) above.
4. Update this file + check off items in VOICE_AND_PRODUCTION.md.
5. Add any new risks discovered to RISK_AND_ISSUE_MAP.md.
