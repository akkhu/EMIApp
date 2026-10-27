#!/bin/bash
# EMI Locker POC - one-shot build + deploy.
# RUN THIS ON YOUR MAC (it needs your Android SDK + release.keystore).
# From repo root:  ./poc_build_deploy.sh
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
APP_DIR="$ROOT/EmiLockerApp"
PUB="$ROOT/emi-admin-dashboard/public"
APK_OUT="$APP_DIR/app/build/outputs/apk/release/app-release.apk"
STABLE_APK="$PUB/emilocker.apk"

echo "==> Checking Java (AGP 8.13 needs JDK 17+)"
if ! java -version 2>&1 | grep -qE '"(17|18|19|2[0-9])'; then
  JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  if [ -d "$JBR" ]; then
    export JAVA_HOME="$JBR"; export PATH="$JAVA_HOME/bin:$PATH"
    echo "    Using Android Studio JDK: $JAVA_HOME"
  else
    echo "!!  Need JDK 17. Open Android Studio > Settings > Build > Gradle and set JDK 17, or set JAVA_HOME."; exit 1
  fi
fi


echo "==> Checking Factory Reset Protection account"
if grep -q 'listOf(\s*""' "$APP_DIR/app/src/main/java/com/scmemilocker/app/Config.kt" 2>/dev/null || \
   grep -qE 'listOf\(\s*$' "$APP_DIR/app/src/main/java/com/scmemilocker/app/Config.kt" && grep -qE '^\s*""' "$APP_DIR/app/src/main/java/com/scmemilocker/app/Config.kt"; then
  echo "!!  WARNING: FRP_ACCOUNT_IDS in Config.kt is empty."
  echo "    A recovery-mode reset will REMOVE the lock. See POC_SETUP.md section 8."
  read -p "    Continue anyway? [y/N] " ans; [ "$ans" = "y" ] || exit 1
fi

echo "==> Building signed release APK"
cd "$APP_DIR"
./gradlew --no-daemon clean assembleRelease
cd "$ROOT"

[ -f "$APK_OUT" ] || { echo "!! build output not found: $APK_OUT"; exit 1; }

echo "==> Publishing APK to dashboard at stable path /emilocker.apk"
mkdir -p "$PUB"
# remove old versioned apks to avoid confusion
rm -f "$PUB"/EmiLockerApp_v*.apk 2>/dev/null || true
cp "$APK_OUT" "$STABLE_APK"

echo "==> Verifying signing-cert checksum matches the QR (checksum.json)"
KS="$APP_DIR/app/release.keystore"
HEX=$(keytool -list -v -keystore "$KS" -alias emilocker -storepass password 2>/dev/null \
  | grep "SHA256:" | sed 's/.*SHA256: //' | tr -d ':' | tr '[:upper:]' '[:lower:]')
B64=$(echo "$HEX" | xxd -r -p | base64 | tr '+/' '-_' | tr -d '=')
WANT=$(grep -o '"signatureChecksum": *"[^"]*"' "$ROOT/emi-admin-dashboard/src/checksum.json" | sed 's/.*"\([^"]*\)"$/\1/')
echo "    keystore cert checksum : $B64"
echo "    checksum.json expects  : $WANT"
echo "    (QR intentionally uses Google's Play signing key; checksum.json is not changed.)"
echo "    APK sha256: $(shasum -a 256 "$STABLE_APK" | awk '{print $1}')"

echo "==> Committing and pushing (Vercel will auto-deploy)"
cd "$ROOT"
git add -f "$STABLE_APK"
git add -A
git commit -m "POC build $(date +%Y%m%d-%H%M): lock/unlock/release + USB/dev/safe-mode blocks, fixed QR hosting" || echo "    (nothing to commit)"
git push

echo ""
echo "============================================================"
echo " DONE. Wait ~1-2 min for Vercel, then:"
echo "   1. Open your dashboard:  https://emi-app-7pq1.vercel.app"
echo "   2. Confirm the APK is live (should DOWNLOAD, not show a page):"
echo "        https://emi-app-7pq1.vercel.app/emilocker.apk"
echo "   3. Click 'Generate Onboarding QR' and provision a factory-reset phone."
echo "============================================================"
