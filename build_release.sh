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

# 5. Calculate New Checksum
echo "🔐 Calculating new APK checksum..."
HASH=$(shasum -a 256 emi-admin-dashboard/public/EmiLockerApp.apk | awk '{print $1}')
URL_SAFE_B64=$(echo -n $HASH | xxd -r -p | base64 | tr '+/' '-_' | tr -d '=')
echo "{\"checksum\": \"$URL_SAFE_B64\"}" > emi-admin-dashboard/src/checksum.json

# 6. Update Dashboard Version JSON
echo "{\"versionName\": \"$NEW_VERSION_NAME\", \"versionCode\": $NEW_VERSION_CODE}" > emi-admin-dashboard/src/version.json

# 7. Commit changes
echo "💾 Committing changes to Git..."
git add .
git commit -m "Bump version to $NEW_VERSION_NAME (Code: $NEW_VERSION_CODE)"

echo ""
echo "🎉 Done! Everything is built, copied, and committed!"
echo "👉 Now just push your changes in GitHub Desktop to trigger the Vercel update."
