# The J.A.R.V.I.S. Map — B.I.H.'s North Star

*"My phone is my suit of armor and helper in all ways." — the user, 2026-11-11*

*The user's founding vision (2026-11-11): "I said to myself, if I had
something like J.A.R.V.I.S. on my phone, things would be a whole lot
easier to fill out forms and everything else. Keep in mind the high
functionality of J.A.R.V.I.S. from the Iron Man movies."*

This file maps what J.A.R.V.I.S. does in the films to B.I.H. — what's
shipped, what's planned, and what we deliberately do differently (our
J.A.R.V.I.S. is **private and on-device**: Tony's data never leaves the
suit; the user's data never leaves the phone).

Sources: the films themselves + a capabilities overview the user found
and pasted (2026-11-11) — home management, suit operations, tactical
support, combat/strategy advice, system diagnostics, remote control of
multiple suits, and vast data management.

Status: ✅ shipped · 🔨 built, testing · 🗺️ planned · ⛔ blocked/conflicts

| J.A.R.V.I.S. can… | Movie moment | B.I.H. today | Path |
|---|---|---|---|
| Talk naturally, free-form | Every scene | 🗺️ Rule-based assistant shipped; real conversation needs on-device AI | Gemini Nano/LiteRT (on-device AI chat, part 2) |
| Wake on his name | "J.A.R.V.I.S.?" | ⛔ True wake words are phone-maker only; always-on mic breaks our privacy model | ✅ Built instead: default-assistant gesture + Quick Settings tile. Revisit if Android opens wake words |
| Remind proactively | "Sir, your 3 o'clock…" | ✅ Reminders with exact on-time alarms (v0.1.10) | Done |
| Read & explain anything on screen | Tony browses holo-files | 🔨 Read-this-screen with controls + folders (v0.1.7–8); explaining needs the on-device LLM | LLM "explain simply" button on the reader |
| Fill out forms | Lab automation | 🔨 Autofill + accessibility screen-fill + paperwork wizard (needs on-device testing) | On-device tests, then beta |
| Sign documents | — | 🔨 Drawn e-signature stored encrypted; typed name in signature boxes | Stamp into PDF exports (Phase 4) |
| Know Tony — memory that sticks | "You always forget…" | ✅ Notes with folders, chat memory, profile | Keep growing recall intents ("what did I…") |
| Walk through protocols step-by-step | "House Party Protocol" | ✅ Routines with one-step-at-a-time walkthroughs | Done; voice-driven next |
| Monitor vitals / wellbeing | Suit telemetry | 🗺️ | Gentle check-ins ("How's your head today?"), symptom notes — on-device only |
| Call/message people | "Get me Pepper." | 🗺️ | Assistant intent → opens the phone/message app with confirmation first |
| Warn of danger | Threat detection | ✅ Crisis detection (always runs first) + calm mode for overload | Keep sharpening |
| Run diagnostics on himself | Suit status reports | 🗺️ | "Self-check" command: is my brain (API) up, storage OK, backups? |
| Look anything up | Instant knowledge | 🗺️ | On-device LLM first; optional web lookup later — privacy decision with the user |
| Translate languages | — | 🗺️ | On-device ML Kit translation fits the privacy model |
| Tell voices apart | Party scenes | 🗺️ Phase 5+ | On-device diarization (Pyannote) — who-said-what conversation notes |
| Dry humor & personality | "Sir, I'm going to have to ask you to exit the donut." | 🗺️ | Warmth yes, sass carefully — calm and clarity first for our users |
| Control the house/suit | Lab, suit deployment | 🗺️ | Phone-level: open apps, start calls, toggle flashlight — via intents, always confirmed |
| Manage the smart home | Mansion lighting, security | ⛔ OFF by project decision: cast/mirroring abuse is a real-world vector (risk S16) — no smart-home or cast features unless explicitly requested | n/a |
| Real-time tactical support | Live data mid-mission | 🗺️ | In-the-moment help during appointments/forms: the overload helper + "what does this mean?" while it's on screen |
| Strategic advice | Battle scenario analysis | 🗺️ | "Help me think this through" — pros/cons walkthroughs, one small step at a time (needs on-device LLM) |
| Remote-control many suits at once | Coordinated suit fleets | 🗺️ | Multi-device (phone + tablet) — would need end-to-end-encrypted sync; big privacy decision, user decides |
| Self-diagnostics with alerts | Suit status warnings | 🗺️ | Self-check command + gentle alerts ("my brain stopped — tap here to restart") |

## Rules that make it OUR J.A.R.V.I.S.
1. **Private like the suit** — on-device first; no cloud brain.
2. **Calm like a good assistant** — one step at a time, low cognitive load.
3. **The user is Tony** — B.I.H. advises and prepares; the user decides
   (confirmation screens before anything irreversible).
4. **Every capability gets a risk-map entry** before it ships.
