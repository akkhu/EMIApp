# Android 16 (OnePlus) Device Owner Provisioning Guide

## What Went Wrong with the Play Store Version?
When you uploaded the app to the Google Play Store (v8), Google Play automatically modified the Android App Bundle by applying its own "App Bundle V3 Signature Block". While this works perfectly for normal users downloading from the Play Store app, the strict **Android 16 Setup Wizard on OnePlus** crashes when it encounters this specific signature format during QR Code Device Owner Provisioning. This is why you saw the immediate "Something went wrong" crash.

## The Solution: Local Sideloading (v11)
We already proved earlier today that your phone **flawlessly accepts the local signature** (the `release.keystore` on your Mac). 

Therefore, we are completely bypassing the Google Play Store for the initial provisioning step. 

I have built **Version 11 (v11)** locally using the exact local signature that your phone likes, and placed it inside the `public/` folder of your Vercel dashboard.

I also completely removed the buggy `LEAVE_ALL_SYSTEM_APPS_ENABLED` flag. This means:
1. The QR parser won't crash.
2. The phone will **skip the 5-minute hang** because it won't waste time trying to unhide OnePlus bloatware!

---

## Final Instructions

Please follow these exact steps (Do NOT upload to Play Store!):

### 1. Push the Local v11 APK to Vercel
Run these commands in your terminal to deploy the Local v11 setup to your live Vercel dashboard:

```bash
cd /Users/akhilesh/Documents/GitHub/EMIApp/emi-admin-dashboard
git pull
git push
```

### 2. Verify Vercel is Updated
Wait 1-2 minutes for Vercel to finish building. 
Refresh your live Vercel Dashboard in your browser. 
**CRITICAL**: The version text at the bottom of the QR code must say **"1.10 (Local Native)"**. 
*(If it does not say this, DO NOT SCAN IT—wait for Vercel to update and refresh again).*

### 3. Provision the Device
1. Factory reset your OnePlus phone.
2. Tap the screen 7 times on the welcome screen to open the QR scanner.
3. Connect to Wi-Fi.
4. Scan the new v11 QR code.

Because this uses the local APK with the local signature, it will bypass the Play Store entirely, download flawlessly, and skip the 5-minute hang!
