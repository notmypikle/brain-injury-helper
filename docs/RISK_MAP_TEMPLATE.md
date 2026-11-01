# Risk & Issue Map — TEMPLATE (copy to any new project on day one)

*Living document. Every known or theoretical issue: what it is, how likely,
how bad, and how we resolve or prevent it. Review before every release.*

Severity: 🔴 critical · 🟠 high · 🟡 medium · ⚪ low
Status: ✅ resolved · 🛡️ mitigated · 📋 planned · 👁️ monitored

---

## 1. PROGRAMMING ISSUES
*Things that can break in the code itself.*

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| P1 | (e.g. database contention, race conditions, bad input handling) | | | |
| P2 | (e.g. third-party API changes or outages) | | | |
| P3 | (e.g. unbounded data growth, memory leaks) | | | |

## 2. SECURITY ISSUES
*Ways the app or its users could be attacked or exposed.*

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| S1 | (e.g. unauthenticated endpoints) | | | |
| S2 | (e.g. unencrypted traffic or storage) | | | |
| S3 | (e.g. secrets in code/logs, dependency vulnerabilities) | | | |
| S4 | (e.g. injection: SQL, prompt, XSS) | | | |

## 3. INSTALLATION ISSUES
*Ways setup can fail for a new user or machine.*

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| I1 | (e.g. wrong language/runtime version) | | | |
| I2 | (e.g. missing system dependencies, port conflicts) | | | |
| I3 | (e.g. unclear docs, wrong working directory) | | | |

## 4. OPERATIONAL ISSUES
*Things that go wrong while the app is running in the real world.*

| # | Issue | Sev | Status | Resolution |
|---|---|---|---|---|
| O1 | (e.g. no backups, no monitoring, data corruption) | | | |
| O2 | (e.g. store review rejection, legal/compliance gaps) | | | |
| O3 | (e.g. cost creep, support burden, maintainer burnout) | | | |

---

## HOW TO USE THIS MAP
1. **Before each release:** re-scan every 📋 and 👁️ row; promote or close.
2. **When adding a feature:** add its new risks here in the same PR.
3. **When an issue actually happens:** record what the map missed.
4. **Severity guide:** 🔴 = can hurt a user or lose their data · 🟠 = breaks core function or exposes data · 🟡 = degraded experience · ⚪ = annoyance.
