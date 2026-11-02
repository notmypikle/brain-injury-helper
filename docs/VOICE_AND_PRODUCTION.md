# Voice Architecture & Production Roadmap — B.I.H.

## PART 1: VOICE (talk-to-text & voice operation)

**Decision: voice runs 100% on-device.** Android's built-in `SpeechRecognizer`
(talk-to-text) and `TextToSpeech` (voice output) are free, work offline, and
audio never leaves the phone — matching our privacy-first model.

```
User speaks → Android SpeechRecognizer (on device) → text
                                                      ↓
                                          POST /voice {transcript}
                                                      ↓
User hears reply ← Android TextToSpeech ← {reply_text}
```

- **API side: done.** `POST /voice` accepts a transcript and returns
  `reply_text` + `safety_flag` (crisis detection applies to voice too).
- **Android side (Phase 2–3):** mic button on the chat screen; one tap to
  talk, app speaks the reply. No accounts, no cloud speech services.
- **Why not cloud voice (Whisper etc.)?** Costs money per minute, sends audio
  off-device, needs network. Keep as an optional Plus-tier upgrade later for
  users whose speech the on-device engine struggles with (see risk P5 in
  RISK_AND_ISSUE_MAP.md).
- **Accessibility rule:** text input is always equally available. Voice is an
  option, never a requirement.

## PART 2: CONCEPT → PRODUCTION ROADMAP

### ✅ Done (concept → working foundation)
- [x] Product concept, privacy model, implementation plan
- [x] FastAPI backend: chat with crisis detection, notes, reminders, routines, voice endpoint
- [x] Paperwork wizard: profile auto-fill, form templates, finished-document storage, text export (for forms that can only be filed online)
- [x] Local-first SQLite storage, optional API-key protection
- [x] Test suite (21 tests), pinned dependencies
- [x] Risk & issue map, monetization plan

### 🔨 Next up — Phase 2: Android app (the big one)
- [x] Scaffold Kotlin + Jetpack Compose project (not yet build-verified — see docs/PROJECT_STATUS.md)
- [x] Chat screen (text first) wired to `/chat`
- [x] Today screen: due reminders + routine checklists
- [x] Notes screen wired to `/notes`
- [x] Settings: API address, API key (app lock still pending)
- [x] Mic button + TTS reply (voice per Part 1) — transcript is reviewed in the text box before sending (risk P5)
- [x] Local notifications for reminders (WorkManager; app also creates reminders now)

### 🚀 Phase 3: Beta
- [ ] Closed beta via Play Console (10–20 real users from brain-injury communities)
- [ ] Accessibility audit: TalkBack, large text, one-hand use (launch gate — risk O4)
- [ ] Feedback loop: in-app "was this helpful?" + weekly review
- [ ] Fix, simplify, repeat. Low cognitive load beats features.

### 💰 Phase 4: Monetization & polish (see MONETIZATION.md)
- [ ] Encryption at rest (SQLCipher) — security launch gate
- [ ] Data export/delete (privacy launch gate)
- [ ] Paperwork helper + better AI model → Plus tier
- [ ] Play Store public listing (no medical claims — risk O5)

### 🏢 Phase 5: Scale
- [ ] Caregiver tier (shared view with consent)
- [ ] B2B pilot with rehab clinics
- [ ] Optional local LLM for fully offline AI

## LAUNCH GATES (must be ✅ before public release)
1. All 12+ tests passing, API key auth enabled
2. Encryption at rest + export/delete working
3. Accessibility audit passed
4. Legal: disclaimer reviewed, privacy policy published
5. Every 🔴 row in RISK_AND_ISSUE_MAP.md is 🛡️ or ✅
