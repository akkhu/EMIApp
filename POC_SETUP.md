# EMI Locker — POC Setup & Provisioning Guide

A device-financing (EMI) lock built on Android **Device Owner** (the official
Android Enterprise / DevicePolicyManager framework — the same mechanism used by
PayJoy, Samsung Knox Guard and Google's Device Lock Controller). It provisions
via a QR code on a factory-reset phone, can be locked/unlocked remotely, blocks
the common ways a lock could be bypassed, and can be fully released when the
loan is paid. Works on **Android Go and standard Android** (Android 7 / API 24+).

---

## 1. Why provisioning was failing before (diagnosis)

Everything in the QR math was correct — component name, package checksum (it
matched the APK byte-for-byte), and the APK was properly V1+V2+V3 signed with
your `release.keystore`. The real problems:

1. **The APK was never committed to git**, so Vercel wasn't serving it. During
   setup the phone fetched the download URL, got the dashboard's HTML (or a 404)
   instead of a binary, and provisioning died right after the scan.
2. The QR used **`PACKAGE_CHECKSUM`** (the APK's byte hash), which changes on
   every rebuild — so any new build silently invalidated the QR.

### What changed
- QR now uses **`SIGNATURE_CHECKSUM`** (your signing-cert hash). It's constant
  across rebuilds, so the QR is permanent.
- APK is served at a **stable path**: `/emilocker.apk`, committed to the repo
  and force-added by the deploy script, with correct `Content-Type` headers.
- App stripped to a pure POC: **no login, no customer form**. It silently
  self-registers (device id + FCM token) and hides its icon.
- Added the hard restrictions you asked for (below), enforced from provisioning
  until release.

---

## 2. What the device enforces

Applied the moment the phone is provisioned, and kept until **Release**
(so the lock cannot be broken even while the phone is currently "unlocked"):

| Restriction | Effect |
|---|---|
| `DISALLOW_USB_FILE_TRANSFER` + USB data signaling off (API 31+) | No USB file transfer / data line |
| `DISALLOW_SAFE_BOOT` | Cannot reboot into safe mode |
| `DISALLOW_DEBUGGING_FEATURES` | No developer options / ADB |
| `DISALLOW_FACTORY_RESET` | Cannot factory reset to escape |
| `DISALLOW_APPS_CONTROL` | Cannot force-stop, clear data, or uninstall the app |
| `DISALLOW_ADD_USER`, `DISALLOW_CONFIG_DATE_TIME`, `DISALLOW_MOUNT_PHYSICAL_MEDIA` | Close common bypass routes |

- **Lock** = full-screen kiosk (lock-task pinned; Home/Back/Recents trapped).
- **Unlock** = kiosk removed; hard restrictions stay (loan not finished).
- **Release** = all restrictions cleared, Device Owner relinquished, app icon
  restored, app settings opened so the customer can uninstall.

---

## 3. One-time setup

1. **JDK 17** — needed by AGP 8.13. If you build from Android Studio's terminal
   it's automatic; otherwise the deploy script auto-detects the Android Studio JDK.
2. **(For instant lock) Firebase service account on Vercel** — set env var
   `FIREBASE_SERVICE_ACCOUNT` to the JSON of your Firebase admin key
   (Project Settings → Service accounts → Generate new private key), then redeploy.
   Without it, lock/unlock still works via background polling (up to ~15 min).

---

## 4. Build & deploy (run on your Mac)

> **No Mac needed:** pushing any change under `EmiLockerApp/` to GitHub triggers
> the "Build & publish EMI APK" workflow (`.github/workflows/build-apk.yml`). It
> builds the signed APK on GitHub's servers, publishes `emilocker.apk`, and pushes,
> which redeploys Vercel. You can also run it manually from the repo's
> **Actions** tab → *Build & publish EMI APK* → **Run workflow**.


```bash
cd /Users/akhilesh/Documents/GitHub/EMIApp
./poc_build_deploy.sh
```

This builds the signed APK, publishes it to `/emilocker.apk`, verifies the
signature checksum matches the QR, commits, and pushes. Vercel auto-deploys in
~1–2 min.

**Verify the APK is actually live** (this must *download*, not show a web page):
```
https://emi-app-7pq1.vercel.app/emilocker.apk
```

---

## 5. Generate the QR & provision a phone

1. Open `https://emi-app-7pq1.vercel.app` → **Generate Onboarding QR**.
2. On the target phone: **factory reset** it.
3. On the first "Hi there / Welcome" setup screen, **tap the screen 6–7 times**
   in the same spot → the QR scanner opens.
4. Connect to **Wi-Fi** when asked.
5. **Scan the QR.** The phone downloads the APK, installs it as Device Owner,
   and finishes setup automatically. The app self-registers and disappears.

---

## 6. Test the flow

On the dashboard, the device appears (shown by model). Then:
- **Lock** (red) → phone shows the kiosk within seconds (instant with FCM, else
  on next poll).
- **Unlock** (green) → kiosk clears.
- **Release** (trash) → removes the lock permanently and lets the customer
  uninstall. *You cannot re-lock a released device without factory-resetting it.*

> POC testing escape: on the lock screen, tap the title **10×** quickly to reveal
> a maintenance "Release" button, so you never brick a test phone.

---

## 7. If provisioning still fails — checklist

1. **APK URL** `…/emilocker.apk` downloads a file (not HTML/404). Most failures
   are here — confirm the push deployed and Vercel has no deployment protection
   (Project → Settings → Deployment Protection = off for the APK to be public).
2. **Same domain**: open the dashboard on the exact domain the app calls
   (`emi-app-7pq1.vercel.app`) so the QR's download URL and the app's API match.
3. **Wi-Fi connected** before scanning.
4. **Signature checksum** in `src/checksum.json` matches the keystore (the deploy
   script checks and auto-corrects this).
5. Re-generate the QR after deploy and scan the fresh one.

---

## 8. Factory Reset Protection (survives a recovery-mode reset)

The app blocks reset from Settings, but no app can block a reset from **recovery
mode** (power + volume keys at boot). FRP closes that gap: after such a reset,
the phone refuses to finish setup until someone signs in with **your lender
Google account**. A wiped phone is useless to the customer, and you can recover it.

- Android 11+: official `FactoryResetProtectionPolicy`.
- Android 7–10 (incl. most Android Go): Google Play services legacy method.
- Cleared automatically on **Release**.

### One-time setup
1. Create a **dedicated** Google account for recovery (e.g. `yourco.emi.recovery@gmail.com`).
   Don't use a personal account. Keep the password safe; you'll need it to recover phones.
2. Get its **numeric account ID** (not the email):
   - Open https://developers.google.com/people/api/rest/v1/people/get
   - In "Try this method": `resourceName` = `people/me`, `personFields` = `metadata`
   - Sign in **as the recovery account** and Execute.
   - The response has `"resourceName": "people/1234567890..."` → the number is the ID.
3. Put it in `EmiLockerApp/app/src/main/java/com/scmemilocker/app/Config.kt`:
   ```kotlin
   val FRP_ACCOUNT_IDS: List<String> = listOf("1234567890...")
   ```
4. Rebuild and deploy with `./poc_build_deploy.sh`. Phones already provisioned pick it up on their next boot or app launch.

### Test it (on a test phone only)
Provision → reboot to recovery → wipe data → on setup, it should ask for a
previously-synced Google account. Sign in with the recovery account to unlock.
To re-lock that phone afterwards: factory reset from Settings and scan the QR again.

### Limits
- Needs Google Play services on the phone.
- Doesn't stop someone flashing new firmware with a PC tool. Only Zero-touch,
  Knox Guard, or Device Lock Controller survive that (production path).

---

## 9. IMPORTANT: the QR must serve the Google Play-signed APK

The app is published on Google Play, and Play App Signing re-signs it with
Google's key (`D0:E7:16:63:…`). A copy signed with the local key (`C9:FB:63:DF:…`)
has the same package name but a different signature, so Android refuses it during
QR setup ("Something went wrong" after **Next**). Google's TestDPC worked on the
same phones, which is how this was found.

The QR therefore serves `/emilocker-play.apk` (Google-signed) and uses the Play
signing key checksum `0OcWY9LK7lYVeQCknepeJ36LCrPeGuDJeAIv5l_Jq1I=`.

### Releasing a new app version
1. Push the code; GitHub Actions builds it. Download the **emilocker-aab-for-play**
   artifact from the run.
2. Play Console → Testing → **Internal testing** → Create new release → upload the `.aab`.
3. Play Console → **App bundle explorer** → that version → Downloads →
   **Signed, universal APK**.
4. Replace `emi-admin-dashboard/public/emilocker-play.apk` with it, commit and push.

The QR and checksum never change. Keep Play Console → *Protected with Play* →
**Automatic protection ("Prevent unofficial installs") OFF**, otherwise installs
from the QR are blocked.
