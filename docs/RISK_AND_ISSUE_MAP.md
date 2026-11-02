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
| P2 | Reminder times stored without timezone | 🟠 | 📋 | Store ISO 8601 with explicit offset; Android app sends local time + offset. Add validation in Phase 2. |
| P3 | Crisis regex false negatives (misspellings, other languages) | 🔴 | 🛡️ | Regex is a backstop only. Phase 3+ adds LLM-based detection layer. Never rely on regex alone; emergency button always visible in UI regardless. |
| P4 | Crisis regex false positives (e.g. quoting a song lyric) | 🟡 | 🛡️ | Reply is caring but non-blocking — user can keep chatting. Acceptable tradeoff: over-trigger beats under-trigger. |
| P5 | Speech recognition errors for users with speech impairments | 🔴 | 📋 | Core audience risk. On-device STT quality varies with impaired speech. Mitigate: always offer text input equally, slow confirmation loop, "did I get that right?" repeats. Test with real users early. |
| P6 | Dependency supply-chain break (package yanked/vulnerable) | 🟡 | ✅ | Versions pinned in requirements.txt. Re-run pip audit before releases; Dependabot on GitHub. |
| P7 | Unbounded database growth (notes/reminders forever) | 🟡 | 📋 | Phase 4 export + user-controlled deletion already planned; add DB size to /health later. |
| P8 | Breaking API changes during Android development | 🟡 | 🛡️ | Pydantic models version responses; API versioned (v0.3.0); add `/v1/` prefix before public release. |
| P9 | LLM provider outage once wired in | 🟠 | 📋 | Graceful fallback: cached supportive responses + all local features (notes/reminders/routines) keep working without AI. |
| P10 | Memory leak / unclosed DB connections | 🟡 | ✅ | All connections scoped via `closing()` context manager per request. |
| P11 | Chat history lost when Android kills the app process | 🟡 | 🛡️ | ChatViewModel survives rotation/tab switches; surviving process death needs persisting the chat log locally — planned for Phase 2/3 alongside notes sync. |

## 2. SECURITY ISSUES

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| S1 | API open to anyone on the network | 🟠 | ✅ | Optional `BIH_API_KEY` + `X-API-Key` header (constant-time comparison). Mandatory key once deployed off-device. |
| S2 | Traffic unencrypted (plain HTTP on LAN) | 🟠 | 📋 | Termux/local use is loopback-only. Production: TLS via reverse proxy (Caddy/nginx) or Android Keystore-pinned cert. Never expose plain HTTP to the internet. |
| S3 | Database readable if phone is stolen | 🔴 | 📋 | Phase 4: encryption at rest (SQLCipher) + Android app lock. Documented in PRIVACY_AND_SAFETY.md threats. |
| S4 | API keys stored insecurely on device | 🟠 | 📋 | Android Keystore (per PRIVACY_AND_SAFETY.md). Never in code, never in git. |
| S5 | Prompt injection via web content or notes fed to LLM | 🟠 | 🛡️ | Domain allowlist planned; treat all stored text as untrusted context; system prompt hardening when LLM is wired in. |
| S6 | Sensitive data in logs | 🟠 | 🛡️ | No request-body logging anywhere in the API. Rule: log metadata only, never user text. |
| S7 | Dependency vulnerabilities | 🟡 | 🛡️ | Pinned versions + `pip-audit` in release checklist + GitHub Dependabot alerts. |
| S8 | No data export/delete mechanism yet (GDPR-style rights) | 🟠 | 📋 | Phase 4 explicitly includes export/delete/wipe. Until then, deleting `bih.db` wipes everything — documented for users. |
| S9 | Crisis-data liability (app detects crisis, user harmed anyway) | 🔴 | 🛡️ | Disclaimer in README: not a medical provider. Crisis reply directs to human help immediately. Legal review before public launch. |

## 3. INSTALLATION ISSUES

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| I1 | Wrong Python version (<3.10 breaks `X \| Y` types) | 🟡 | 📋 | State Python 3.10+ requirement in README; add version check on startup. |
| I2 | pip install fails on Termux (native deps) | 🟡 | 🛡️ | All current deps are pure-Python wheels. Keep it that way; if a native dep is ever needed, document Termux pkg steps. |
| I3 | Port 8000 already in use | ⚪ | 📋 | Document `--port` flag in README troubleshooting section. |
| I4 | `uvicorn app.main:app` fails from wrong directory | ⚪ | 🛡️ | README shows `cd services/api` first; conftest.py handles path for tests. |
| I5 | DB file created in unexpected location | ⚪ | 🛡️ | `BIH_DB_PATH` override documented; default is absolute path derived from the package, not cwd. |
| I6 | Android build toolchain complexity (SDK, Gradle) | 🟡 | 📋 | Phase 2: pin Gradle/AGP versions, document exact setup steps, CI build to catch drift. |

## 4. OPERATIONAL ISSUES

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| O1 | No backups of user data | 🟠 | 📋 | Phase 4 encrypted export doubles as backup. Document manual `bih.db` copy for power users. |
| O2 | DB corruption (power loss mid-write) | 🟡 | 🛡️ | SQLite is transactional; enable WAL mode in Phase 2 for extra crash safety. |
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
