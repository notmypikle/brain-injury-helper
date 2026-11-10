# Phone-only setup (no computer needed)

Run B.I.H. entirely on your Android phone: the app + its API ("brain")
both live on the device. Data never leaves the phone.

## Step 1 — Install the B.I.H. app
1. Open the repo's **Actions** tab → newest successful **Android build** →
   download the **bih-debug-apk** artifact (a zip).
2. Open the zip → tap **app-debug.apk** → Install
   (allow "install unknown apps" for your browser if asked).

## Step 2 — Install Termux
1. Get it from https://github.com/termux/termux-app/releases/latest —
   download the file ending in **universal.apk**.
2. ⚠️ Do NOT use the Play Store version — it is outdated and broken.
3. Install it the same way as Step 1.

## Step 3 — Start the API in Termux
Open Termux and paste these **one line at a time**, waiting for each:
```bash
pkg update -y && pkg install -y python git
git clone https://github.com/notmypikle/brain-injury-helper.git
cd brain-injury-helper/services/api
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000
```
When you see `Uvicorn running on http://127.0.0.1:8000`, **leave Termux
open** (Android may stop it if you swipe it away; allow Termux to run in
the background when prompted).

Already cloned? Update instead of cloning:
```bash
cd brain-injury-helper && git pull
```

## Step 4 — Connect the app
1. B.I.H. app → **Settings** tab → API address: `http://127.0.0.1:8000` → **Save**
2. **Chat** tab → type "hello" → Send. A reply means everything works.

## Optional — auto-start the API (never open Termux again)
Install **Termux:Boot** from https://github.com/termux/termux-boot/releases/latest
(the **github-debug.apk** — it must match your GitHub-installed Termux; the
F-Droid build conflicts and won't install. If Play Protect warns, tap
"More details" → "Install anyway").

1. Open the Termux:Boot app once, then close it.
2. In Termux, paste this one line:
   ```bash
   mkdir -p ~/.termux/boot && printf '#!/data/data/com.termux/files/usr/bin/sh\ntermux-wake-lock\ncd ~/brain-injury-helper/services/api && uvicorn app.main:app --host 127.0.0.1 --port 8000\n' > ~/.termux/boot/start-bih.sh && chmod +x ~/.termux/boot/start-bih.sh
   ```
3. Phone Settings → Apps → Termux → Battery → **Unrestricted**.
4. Restart the phone — the API starts by itself ~30 seconds after boot.

## Optional — lock the API down
Set a key before starting uvicorn:
```bash
export BIH_API_KEY="choose-a-long-random-string"
```
then put the same string in the app's Settings → API key.

## Troubleshooting
| Symptom | Fix |
|---|---|
| `pip`/`pkg` command not found | Re-run Step 3 line 1 |
| App says "Can't reach the helper" | Termux closed or uvicorn not running; redo the uvicorn line and check the Settings address is exactly `http://127.0.0.1:8000` |
| Port already in use | An old uvicorn is running — good, just use the app! (Or `pkill -f uvicorn` and restart) |
| Install blocked | Settings → allow "install unknown apps" for the browser you downloaded with |
