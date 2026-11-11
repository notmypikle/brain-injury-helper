# Project Status — B.I.H.

*Where the project stands and what's next. Update this file at the end of
every work session so the next session can pick up without re-discovery.*

Last updated: 2026-11-11

## 🏆 Milestone: ASSISTANT BRAIN VERIFIED ON DEVICE (2026-11-11)
The phrase test PASSED: "remind me to drink water in 30 minutes" got a
real assistant reply (no more echo). Root cause of the earlier echo: the
phone's repo was stuck on the stale `feature/ai-upgrade` branch — fixed
with `git checkout main && git pull`, then a phone restart reloaded
uvicorn with the new code. Follow-up user feedback, both shipped in
v0.1.6 (PRs #21 + #22): "no way to delete chat messages" → Clear chat
button (trash icon + confirm dialog); "it's on military time" → replies
now say "6:00 pm" instead of "18:00" (47 API tests passing).
Pending: user installs v0.1.11 (50k notes / 20k capture, PR #28; on top of v0.1.9–10) (adds the GREEN theme — brain-injury awareness color, PR #25; on top of v0.1.8 reader play/pause/back/next/repeat,
tap-to-jump, save-section-to-folders on top of v0.1.7) + git pull +
phone restart. Accessibility service ENABLEMENT confirmed working on a
Samsung phone (restricted-settings path; risk I7 workaround verified).

## 🏆 Milestone: CALM MODE ON THE USER'S PHONE (2026-11-11)
v0.1.4 installed (the "App not installed" blocker was fixed by
uninstalling the old app first — risk I8's documented workaround,
confirmed working). User's verdict on the "Too much" button: "perfect".
Pending: phone restart to reload the API with the new assistant brain
(git pull done, uvicorn still runs the pre-pull code), then the phrase
test ("remind me to drink water in 30 minutes" → should say "Done!").

## 🏆 Milestone: RUNNING ON THE USER'S PHONE (2026-11-10)
The full stack is live on-device: app (APK from CI, via the `apk` branch
direct-download) + API in Termux (F-Droid build) + Termux:Boot auto-start
(boot script at ~/.termux/boot/start-bih.sh, starts uvicorn ~30–60s after
boot; both Termux and Termux:Boot set to Battery → Unrestricted).
Chat + spoken replies confirmed working end-to-end by the user.
Gotchas encountered (all fixed/documented in docs/PHONE_SETUP.md):
VPN blocking loopback — SOLVED 2026-11-11: exclude the B.I.H. app in the VPN app's split-tunneling settings (B.I.H. only talks to 127.0.0.1, so bypassing the tunnel loses nothing); verified working with VPN ON, sideload signature match
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
| Calm mode ("Too much" button) | ✅ On device | v0.1.4 installed 2026-11-11; user verified the Calm screen: "perfect" |

## User ideas log (things the user asked for — keep building on these)
- **⭐ NORTH STAR (2026-11-11) — J.A.R.V.I.S. from Iron Man.** The whole
  app's origin, in the user's words: "I said to myself, if I had
  something like that on my phone, things would be a whole lot easier to
  fill out forms and everything else that needs to be done on our phones
  and devices. Keep in mind the high functionality of J.A.R.V.I.S."
  (An earlier J.A.R.V.I.S. attempt lived only in a Copilot chat that
  became read-only — another reason everything now lives in this repo.)
  **Full capability map: docs/JARVIS_VISION.md.**
  Every feature should move toward: voice-first, works across all apps,
  fills forms, reads and explains anything, remembers everything —
  while staying private and on-device.
- **2026-11-11 — Reminder form 422 + alarm trust** (real use, day one):
  typing "6:30 p.m." was rejected (form demanded ISO 8601) → reminders
  now accept friendly times; "not sure an alarm will go off" → exact
  AlarmManager alarms when granted (+ Today-screen nudge card), and
  chat-created reminders schedule alarms immediately. PR #26, v0.1.10.
- **2026-11-11 — Save sections into folders of my choosing** (reading a
  document/chat, pause on a section, save just that section into a
  folder): notes have folders (API + migration for existing phones,
  validated names, GET /notes/folders); the read screen's save dialog
  offers folder chips + "Save this section" / "Save whole screen";
  Notes tab shows 📁 folders. PR #24, v0.1.8.
- **2026-11-02 — Cross-app integration + e-signature** (a core need for
  our users): view whatever is on screen, fill it with
  saved private details, e-sign. Built as: BihAccessibilityService
  (screen fill with confirmation, optional Send/Submit tap) + draw-with-
  finger signature pad (encrypted, on-device only) + typed e-signature in
  signature boxes. Risks S12/S13/I7 added to the risk map.
- **2026-11-02 — Online document auto-fill** (both wizard + direct fill):
  paperwork wizard (merged) + autofill service (merged).
- **2026-11-11 — "Can it read what's on the screen when I need it to?"**
  (help understanding texts, emails, online documents): built "Read this
  screen" — the system accessibility button now opens a simple chooser:
  📖 Read this screen (big calm text + optional read-aloud, off by
  default) or ✍️ Fill boxes (existing flow). Same safety rules: reads
  only on the user's tap, never passwords, nothing stored/sent (S15).
  Plain-language EXPLAINING ("what does this letter mean?") queued for
  the on-device LLM (part 2). PR #23, v0.1.7.
- **2026-11-11 — "No way to delete the last chat messages"** (found in
  real use): added a Clear chat button (trash icon beside "Too much",
  confirm dialog → wipes the encrypted history, greets fresh). PR #21,
  v0.1.6. Privacy/control need — supports S8.
- **2026-11-11 — "It's on military time"**: 24-hour times are harder to
  read (accessibility). Assistant replies now use 12-hour am/pm
  ("6:00 pm"). PR #22, v0.1.6.
- **2026-11-10 — Legal paperwork help** (a real need: "there's always
  legal paperwork when dealing with life as a human"): 4 letter templates
  added to the wizard — benefits appeal, formal complaint, records request
  (subject access), adjustments request. Hard rule: letters and
  information only, NEVER legal advice; every template says so and points
  to free qualified help (risk S14, enforced by a test).
- **2026-11-10 — Voice separation / "who said what"** (noise and too much
  simultaneous stimulation are genuine daily barriers for our users —
  treat as a CORE accessibility need, not a nice-to-have): future features — conversation
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
  **MERGED (PR #19, merged by Copilot at the user's request) and
  published as B.I.H. v0.1.4 on the `apk` branch.** 46 API tests
  passing (6 new). Pending: on-device test by the user.
- **Published B.I.H. v0.1.3 to the `apk` branch** (built from CI run
  42922012521, PR #18 merge): real local assistant + hardened chat
  persistence. The direct download now has the assistant build.
- **Default API address fixed (PR #20, merged; v0.1.5 on the `apk`
  branch):** fresh installs pointed at the emulator address (10.0.2.2) —
  the user hit this after their reinstall. Now defaults to
  `http://127.0.0.1:8000` (API in Termux on the same phone). No action
  needed on the user's current install (address already saved).
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

## Session paused 2026-11-11 ~03:05 UTC
User on a break, may return tonight. Everything below is current. The
ONE parked action: the user installs v0.1.11 (4-step dance — NOTE:
history was rewritten for privacy 2026-11-11, so the Termux step is now
cd ~/brain-injury-helper && git fetch origin && git reset --hard origin/main
(plain git pull will fail — histories differ) → uninstall old app → install from apk branch → restart phone),
then taps "Allow exact alarms" on the Today card. VPN split-tunneling
is set up and verified. New tonight (all merged): v0.1.10 friendly
reminder times + exact alarms; v0.1.11 big-note wiggle room; note cap
fix (PR #27); JARVIS_VISION.md created; smart home/cast permanently
OFF (risk S16 — real-world cast/mirroring abuse exists; cast features
stay off unless explicitly requested).

## Where we left off / next steps (in order)
1. ✅ DONE 2026-11-10: app installed on the user's phone, API running in
   Termux, auto-start on boot working, chat + voice verified end-to-end.
2. **On-device feature tests** (user, at their own pace): autofill service
   (Settings → System → Autofill service → B.I.H.), screen-fill service
   (Settings → Accessibility → B.I.H.; Android 13+ restricted settings —
   risk I7), reminder notifications, Forms wizard, **calm mode** (v0.1.4:
   "Too much" button on Chat/Today, and saying "it's too much" to the
   assistant).
3. ✅ DONE 2026-11-10: PR #19 (calm mode) merged, v0.1.4 published to the
   `apk` branch (CI run 42926370246).
4. **Overstimulation support** (user's core need, 2026-11-10): ✅ "too
   much" calm-mode button DONE (PR #19 merged, v0.1.4; on-device test
   pending). Later: who-said-what / focus-one-voice.
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
