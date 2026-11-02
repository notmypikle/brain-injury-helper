# Android App (Kotlin + Jetpack Compose)

The B.I.H. Android app: Chat, Today, Notes, Settings — with on-device
voice (talk-to-text and spoken replies; audio never leaves the phone).

## Screens
- **Chat** — talk or type; B.I.H. replies on screen and aloud. Crisis
  language is detected server-side and answered with care + helplines.
- **Today** — due reminders (tap "Done") and step-by-step routines.
- **Notes** — the memory journal: save anything, delete anytime.
- **Settings** — API address, API key, voice on/off.

## Build & run (beginner-friendly)
1. Install **Android Studio** (free): https://developer.android.com/studio
2. Start the API on the same Wi-Fi network as your phone:
   ```bash
   cd services/api
   pip install -r requirements.txt
   uvicorn app.main:app --host 0.0.0.0 --port 8000
   ```
   Optional but recommended: `BIH_API_KEY=choose-a-secret uvicorn ...`
3. In Android Studio: **File → Open** → select this `apps/android` folder.
   Android Studio downloads the Gradle wrapper and SDK automatically on
   first sync (one-time, a few minutes).
4. Plug in your phone (USB debugging on) or start an emulator, press ▶️.
5. In the app's **Settings** tab, set the API address to your computer's
   IP, e.g. `http://192.168.1.10:8000`, and the API key if you set one.

## Architecture
```
MainActivity ── BihNavHost (bottom tabs)
  ├─ ui/ChatScreen.kt      ← voice/VoiceManager.kt (SpeechRecognizer + TTS)
  ├─ ui/TodayScreen.kt
  ├─ ui/NotesScreen.kt
  └─ ui/SettingsScreen.kt  ← data/SettingsRepository.kt (DataStore)
api/BihApi.kt + ApiClient.kt  ← Retrofit, mirrors services/api/app/main.py
```

Notes:
- `usesCleartextTraffic` is enabled only so the app can reach a local
  `http://` API on your own network. Remove it when the API gets HTTPS
  (tracked as risk S2 in `docs/RISK_AND_ISSUE_MAP.md`).
- Microphone permission is requested at runtime, only when you tap 🎙️.
