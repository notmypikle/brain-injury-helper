# Android Prototype (Jetpack Compose)

This is a starter Android app skeleton for B.I.H. with:
- biometric unlock (fingerprint/face when available)
- backup 6-digit PIN

## What it does now
- First launch: asks user to create a 6-digit PIN.
- Later launches: unlock with biometric or PIN.
- After unlock:
  - Chat mode calls `/chat`
  - Paperwork mode calls `/paperwork/analyze`
  - Call support mode:
    - build a short advocacy call script
    - open phone dialer with pre-filled number
  - "Hold my thought" + "Restore thought"
  - Auto-save while typing (about every 1.2 seconds)
  - "Talk and save thought" (speech-to-text)

## Security choices in this prototype
- PIN is never stored as plain text.
- PIN is hashed with PBKDF2 + random salt.
- Salt + hash are stored in encrypted shared preferences.

## Build locally (Android Studio)
1. Open the `apps/android` folder in Android Studio.
2. Let Gradle sync.
3. Run on a device with biometrics enrolled (or use PIN fallback).
