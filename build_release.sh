#!/bin/bash
set -e

if [ -z "$1" ]; then
  echo "❌ Error: Missing version name."
  echo "Usage: ./build_release.sh <new_version_name>"
  echo "Example: ./build_release.sh 1.2"
  exit 1
fi

NEW_VERSION_NAME=$1

# 1. Increment Version Code
GRADLE_FILE="EmiLockerApp/app/build.gradle.kts"
CURRENT_VERSION_CODE=$(grep 'versionCode =' "$GRADLE_FILE" | awk '{print $3}')
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

sed -i '' "s/versionCode = $CURRENT_VERSION_CODE/versionCode = $NEW_VERSION_CODE/" "$GRADLE_FILE"
sed -i '' "s/versionName = \".*\"/versionName = \"$NEW_VERSION_NAME\"/" "$GRADLE_FILE"

echo "✅ Bumped version to Code: $NEW_VERSION_CODE, Name: $NEW_VERSION_NAME"

# 2. Build AAB & APK
echo "🚀 Building Android App Bundle & APK..."
cd EmiLockerApp
./gradlew clean bundleRelease assembleRelease
cd ..

# 3. Update PlayStoreAssets.zip
echo "📦 Updating PlayStoreAssets.zip..."
zip -j ~/Desktop/PlayStoreAssets.zip EmiLockerApp/app/build/outputs/bundle/release/app-release.aab

# 4. Copy APK to Dashboard
echo "📦 Copying APK to Vercel Dashboard..."
cp EmiLockerApp/app/build/outputs/apk/release/app-release.apk emi-admin-dashboard/public/EmiLockerApp.apk

# 5. Extract Signing Certificate Checksum
# The QR code needs the SHA-256 of the SIGNING CERTIFICATE (not the APK itself).
# This checksum stays constant as long as the signing key doesn't change.
echo ""
echo "🔐 Extracting signing certificate checksum..."

# Method A: From the local release keystore
KEYSTORE_PATH="EmiLockerApp/app/release.keystore"
KEY_ALIAS="emilocker"
STORE_PASSWORD="password"

if [ -f "$KEYSTORE_PATH" ]; then
  # Extract SHA-256 fingerprint from the signing certificate
  SHA256_HEX=$(keytool -list -v -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS" -storepass "$STORE_PASSWORD" 2>/dev/null \
    | grep "SHA256:" \
    | sed 's/.*SHA256: //' \
    | tr -d ':' \
    | tr '[:upper:]' '[:lower:]')
  
  if [ -n "$SHA256_HEX" ]; then
    # Convert hex to URL-safe Base64 (required by Android provisioning)
    SIGNATURE_CHECKSUM=$(echo "$SHA256_HEX" | xxd -r -p | base64 | tr '+/' '-_' | tr -d '=')
    echo "   Local keystore signature checksum: $SIGNATURE_CHECKSUM"
  else
    echo "   ⚠️  Could not extract SHA256 from local keystore"
    SIGNATURE_CHECKSUM=""
  fi
else
  echo "   ⚠️  Local keystore not found at $KEYSTORE_PATH"
  SIGNATURE_CHECKSUM=""
fi

# Write checksum to JSON (only signatureChecksum is needed now)
if [ -n "$SIGNATURE_CHECKSUM" ]; then
  cat > emi-admin-dashboard/src/checksum.json <<EOF
{
  "signatureChecksum": "$SIGNATURE_CHECKSUM"
}
EOF
  echo "   ✅ Updated checksum.json with signing certificate checksum"
else
  echo "   ⚠️  Keeping existing checksum.json"
  cat emi-admin-dashboard/src/checksum.json
fi

# 6. Update Dashboard Version JSON
echo "{\"versionName\": \"$NEW_VERSION_NAME\", \"versionCode\": $NEW_VERSION_CODE}" > emi-admin-dashboard/src/version.json

# 7. Commit changes
echo "💾 Committing changes to Git..."
git add .
git commit -m "Bump version to $NEW_VERSION_NAME (Code: $NEW_VERSION_CODE)"

echo ""
echo "🎉 Done! AAB and local APK built successfully."
echo ""
echo "⚠️  IMPORTANT STEPS:"
echo ""
echo "   1. Upload the AAB from ~/Desktop/PlayStoreAssets.zip to Play Console"
echo ""
echo "   2. Get the GOOGLE PLAY signing certificate checksum:"
echo "      → Play Console → Setup → App signing → App signing key certificate"
echo "      → Copy the SHA-256 fingerprint"
echo "      → Run this command to convert it to URL-safe Base64:"
echo ""
echo "      echo '<SHA256_HEX_WITHOUT_COLONS>' | xxd -r -p | base64 | tr '+/' '-_' | tr -d '='"
echo ""
echo "      → If it differs from the local keystore checksum, update:"
echo "        emi-admin-dashboard/src/checksum.json"
echo ""
echo "   3. If Google re-signed your APK (Play App Signing):"
echo "      → Go to Play Console → App Bundle Explorer"
echo "      → Download the Google-signed APK"
echo "      → Rename to EmiLockerApp.apk"
echo "      → Replace emi-admin-dashboard/public/EmiLockerApp.apk"
echo "      → Commit and push again"
echo ""
echo "   4. Push your changes in GitHub Desktop to trigger the Vercel update."
echo ""
echo "   Current checksum.json:"
cat emi-admin-dashboard/src/checksum.json
echo ""
