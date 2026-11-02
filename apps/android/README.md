# Android App (Kotlin + Jetpack Compose)

Screens: **Chat, Today, Notes, Routines, Forms, Settings** — all wired to the API.
The **Forms** tab is the paperwork wizard: save your details once, pick a
form, answer what's left, then copy the finished lines into the online form.

## First-time setup (step by step)
1. Install **Android Studio** (free, from developer.android.com/studio).
2. Open Android Studio → **Open** → pick this folder (`apps/android`).
3. Wait for Gradle sync (first one downloads a lot — that's normal).
   - Android Studio will generate the Gradle wrapper (`gradlew`) automatically.
   - Versions are pinned in `build.gradle.kts` files (risk I6).
4. Start the API on your computer:
   ```bash
   cd services/api
   uvicorn app.main:app --port 8000
   ```
5. Run the app in an emulator (▶ button). The default API address
   `http://10.0.2.2:8000` already points the emulator at your computer.
   - On a real phone on the same Wi-Fi: set the API address in the app's
     **Settings** tab to your computer's LAN IP, e.g. `http://192.168.1.10:8000`,
     and start uvicorn with `--host 0.0.0.0`.
6. If you set `BIH_API_KEY` on the server, put the same key in Settings.

## What's here
- `app/src/main/java/com/bih/app/MainActivity.kt` — bottom navigation between the 5 screens
- `.../data/ApiClient.kt` — all API calls (chat, notes, reminders, routines)
- `.../data/SettingsStore.kt` — API address + key storage
- `.../ui/screens/` — one file per screen

## Not done yet (see docs/PROJECT_STATUS.md)
- Mic button + spoken replies (voice, on-device)
- Local notifications for reminders
- App lock / Keystore for the API key
