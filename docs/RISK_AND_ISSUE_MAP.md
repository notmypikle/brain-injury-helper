# Risk & Issue Map — B.I.H.

*Living document. Every known or theoretical issue: what it is, how likely,
how bad, and how we resolve or prevent it. Review before every release.*
*Template for reuse across projects: `docs/RISK_MAP_TEMPLATE.md`.*

Severity: 🔴 critical · 🟠 high · 🟡 medium · ⚪ low
Status: ✅ resolved · 🛡️ mitigated · 📋 planned · 👁️ monitored

---

## 1. PROGRAMMING ISSUES

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| P1 | SQLite write contention (two requests write at once) | 🟡 | 🛡️ | SQLite serializes writes; single-user app makes collisions rare. If we ever go multi-user, migrate to PostgreSQL. |
| P2 | Reminder times stored without timezone | 🟠 | 🛡️ | API validates ISO 8601 (422 on invalid). Offset preferred; naive values treated as user's local time. Android app should send local time + offset. |
| P3 | Crisis regex false negatives (misspellings, other languages) | 🔴 | 🛡️ | Regex is a backstop only. Phase 3+ adds LLM-based detection layer. Never rely on regex alone; emergency button always visible in UI regardless. |
| P4 | Crisis regex false positives (e.g. quoting a song lyric) | 🟡 | 🛡️ | Reply is caring but non-blocking — user can keep chatting. Acceptable tradeoff: over-trigger beats under-trigger. |
| P5 | Speech recognition errors for users with speech impairments | 🔴 | 🛡️ | Mitigations built: text input always equally available; mic transcript lands in the text box for review before sending (never auto-sent). Still needs testing with real users in Phase 3 beta. |
| P11 | Dictated audio could leave the device (default speech recognizer may be server-side), contradicting the privacy doc | 🟠 | ✅ | Found by security review 2026-11-10, fixed same day: mic requests on-device recognition (EXTRA_PREFER_OFFLINE) and warns the user first if the device can't do it. |
| P6 | Dependency supply-chain break (package yanked/vulnerable) | 🟡 | ✅ | Versions pinned in requirements.txt. Re-run pip audit before releases; Dependabot on GitHub. |
| P7 | Unbounded database growth (notes/reminders forever) | 🟡 | 📋 | Phase 4 export + user-controlled deletion already planned; add DB size to /health later. |
| P8 | Breaking API changes during Android development | 🟡 | 🛡️ | Pydantic models version responses; API versioned (v0.3.0); add `/v1/` prefix before public release. |
| P9 | LLM provider outage once wired in | 🟠 | 📋 | Graceful fallback: cached supportive responses + all local features (notes/reminders/routines) keep working without AI. |
| P10 | Memory leak / unclosed DB connections | 🟡 | ✅ | All connections scoped via `closing()` context manager per request. |
| P12 | Calm mode ("too much") could mask a real crisis — user says "it's too much, I want to die" and gets grounding steps instead of crisis help | 🔴 | 🛡️ | Crisis detection runs in main.py BEFORE the assistant (incl. the overload intent) and always wins — enforced by test (test_calm.py). Calm steps are non-blocking and never replace the crisis reply. |

## 2. SECURITY ISSUES

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| S1 | API open to anyone on the network | 🟠 | ✅ | Optional `BIH_API_KEY` + `X-API-Key` header (constant-time comparison). Mandatory key once deployed off-device. |
| S2 | Traffic unencrypted (plain HTTP on LAN) | 🟠 | 📋 | Termux/local use is loopback-only. Production: TLS via reverse proxy (Caddy/nginx) or Android Keystore-pinned cert. Never expose plain HTTP to the internet. |
| S3 | Database readable if phone is stolen | 🔴 | 🛡️ | Partially mitigated today: Android full-disk encryption (default on all modern phones with a lock screen) protects `bih.db`, and the app's on-device secrets are Keystore-encrypted (S4). Phase 4 adds SQLCipher + app lock as belt-and-braces. |
| S4 | API keys stored insecurely on device | 🟠 | ✅ | API key and cached profile encrypted with the Android Keystore (hardware-backed, non-extractable AES-256-GCM). Never in code, never in git. |
| S5 | Prompt injection via web content or notes fed to LLM | 🟠 | 🛡️ | Domain allowlist planned; treat all stored text as untrusted context; system prompt hardening when LLM is wired in. |
| S6 | Sensitive data in logs | 🟠 | 🛡️ | No request-body logging anywhere in the API. Rule: log metadata only, never user text. |
| S7 | Dependency vulnerabilities | 🟡 | 🛡️ | Pinned versions + `pip-audit` in release checklist + GitHub Dependabot alerts. |
| S8 | No data export/delete mechanism yet (GDPR-style rights) | 🟠 | 🛡️ | Done: `GET /export` downloads everything as JSON; `DELETE /data` wipes all tables. Deleting `bih.db` also still works. Remaining: encrypted export (Phase 4). |
| S9 | Crisis-data liability (app detects crisis, user harmed anyway) | 🔴 | 🛡️ | Disclaimer in README: not a medical provider. Crisis reply directs to human help immediately. Legal review before public launch. |
| S10 | Profile & paperwork documents concentrate PII/medical data (name, DOB, conditions, medications) | 🔴 | 🛡️ | Local-only storage, never sent to any LLM/cloud by default. Exported documents are plain text — warn users to store/share them carefully. Phase 4 encryption at rest (S3) covers these tables too. Field values are length-capped and keys whitelisted to a safe pattern. |
| S11 | Autofill service could leak profile data into wrong app/website | 🟠 | 🛡️ | Built with conservative field matching; passwords/usernames/cards never touched; nothing fills without the confirmation screen showing every field+value; service reads only the local profile cache (no network); `onSaveRequest` ignored so nothing typed on sites is captured. Verify on a real device before release. |
| S12 | Accessibility service can read/act on any screen (very powerful permission) | 🔴 | 🛡️ | User-requested (Nov 2026: cross-app help needed due to brain injury). Mitigations: opt-in only (user enables it in system settings); never monitors continuously — only reads the screen when the user taps the accessibility button; confirmation screen shows every field+value before filling; only fills EMPTY boxes; password fields excluded; no network calls in the service. Play Store requires an accessibility disclosure — B.I.H. qualifies as a genuine assistive app. |
| S13 | Stored e-signature could be misused if stolen | 🟠 | 🛡️ | Signature strokes are encrypted with the Android Keystore (same as profile cache) and never uploaded. Typed "/s/ name" used for online signature boxes; drawn-signature PDF stamping is future work that will embed at export time only. |
| S14 | Legal paperwork feature mistaken for legal advice (unauthorized practice of law; user relies on wrong info) | 🟠 | 🛡️ | User-requested (Nov 2026). Hard rule: B.I.H. provides LETTERS and plain-language information only, never case-specific advice. Every legal template carries "not legal advice" + points to free qualified help (enforced by a test). No jurisdiction-specific legal claims anywhere. Same pattern as the medical disclaimer. |
| S15 | "Read this screen" could speak private content aloud where others can hear | 🟡 | 🛡️ | User-requested 2026-11-11. Read-aloud is OFF by default and only ever starts from the user's deliberate tap; password fields are never read; the screen text is never stored or sent (memory only, service makes no network calls). |

## 3. INSTALLATION ISSUES

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| I1 | Wrong Python version (<3.10 breaks `X \| Y` types) | 🟡 | ✅ | README states Python 3.10+ requirement. |
| I2 | pip install fails on Termux (native deps) | 🟡 | 🛡️ | All current deps are pure-Python wheels. Keep it that way; if a native dep is ever needed, document Termux pkg steps. |
| I3 | Port 8000 already in use | ⚪ | 📋 | Document `--port` flag in README troubleshooting section. |
| I4 | `uvicorn app.main:app` fails from wrong directory | ⚪ | 🛡️ | README shows `cd services/api` first; conftest.py handles path for tests. |
| I5 | DB file created in unexpected location | ⚪ | 🛡️ | `BIH_DB_PATH` override documented; default is absolute path derived from the package, not cwd. |
| I6 | Android build toolchain complexity (SDK, Gradle) | 🟡 | 🛡️ | Gradle/AGP versions pinned; CI build (GitHub Actions) compiles the app on every push/PR and caught 4 compile errors before any user saw them. |
| I7 | Android 13+ blocks accessibility services for sideloaded APKs ("Restricted settings") | 🟠 | 📋 | User must allow it once: Settings → Apps → B.I.H. → ⋮ menu → "Allow restricted settings", then enable the service. Documented in apps/android/README.md. Gone once distributed via Play Store. |
| I8 | APK update installs fail ("App not installed" / installer errors) | 🟠 | 🛡️ | Likely cause: each CI build signs with a FRESH debug key, so Android refuses to update over a differently-signed install. Fix for users: uninstall the old B.I.H. first (WARNING: wipes app data — Keystore-encrypted chat/profile), then install the new APK. Also check GitHub artifacts download as .zip — extract the .apk before tapping. Proper fix planned: pin a persistent upload key in CI secrets. Reported by user 2026-11-10. |

## 4. OPERATIONAL ISSUES

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| O1 | No backups of user data | 🟠 | 📋 | Phase 4 encrypted export doubles as backup. Document manual `bih.db` copy for power users. |
| O2 | DB corruption (power loss mid-write) | 🟡 | ✅ | SQLite is transactional; WAL mode enabled for extra crash safety. |
| O3 | No crash reporting in production | 🟡 | 📋 | Add privacy-preserving crash reporting (opt-in, no message content) before public launch. |
| O4 | Accessibility regressions (our users depend on it) | 🔴 | 📋 | Accessibility is a launch gate, not a feature: TalkBack testing, large-text support, WCAG AA audit before release. |
| O5 | App Store / Play review rejection | 🟡 | 📋 | Medical-adjacent claims are the risk: keep "not a medical provider" framing everywhere, no diagnostic claims in store listing. |
| O6 | User data sovereignty questions (who owns the data?) | 🟡 | ✅ | Local-first: data lives on the user's device, deletable by deleting the app/DB. Written into PRIVACY_AND_SAFETY.md. |
| O7 | Maintainer bus factor = 1 | 🟡 | 👁️ | Keep docs current (this file, plan, privacy doc) so a future contributor can pick up the project. |
| O8 | Cost creep once LLM is wired in | 🟡 | 📋 | Adopt the cost-modeling approach from the companion project: per-message budget, cached fallbacks, local model option (Phase 4). |

---

## HOW TO USE THIS MAP
1. **Before each release:** re-scan every 📋 and 👁️ row; promote or close.
2. **When adding a feature:** add its new risks here in the same PR.
3. **When an issue actually happens:** record what the map missed.
4. **New project?** Copy `RISK_MAP_TEMPLATE.md` and fill it in on day one.
