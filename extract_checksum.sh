#!/bin/bash
# Helper script to extract the correct signing certificate checksum
# from either the local keystore or a Google Play-signed APK.
#
# Usage:
#   ./extract_checksum.sh                    # From local keystore
#   ./extract_checksum.sh /path/to/play.apk  # From a downloaded APK
#   ./extract_checksum.sh --play-hex "AB:CD:EF:..."  # From Play Console SHA-256

set -e

echo "🔐 EMI Locker - Signing Certificate Checksum Extractor"
echo ""

# Method 1: From Play Console SHA-256 hex string
if [ "$1" == "--play-hex" ] && [ -n "$2" ]; then
  SHA256_HEX=$(echo "$2" | tr -d ':' | tr '[:upper:]' '[:lower:]')
  CHECKSUM=$(echo "$SHA256_HEX" | xxd -r -p | base64 | tr '+/' '-_' | tr -d '=')
  echo "✅ Play signing certificate checksum (URL-safe Base64):"
  echo "   $CHECKSUM"
  echo ""
  echo "Updating checksum.json..."
  cat > emi-admin-dashboard/src/checksum.json <<EOF
{
  "signatureChecksum": "$CHECKSUM"
}
EOF
  echo "✅ Updated emi-admin-dashboard/src/checksum.json"
  exit 0
fi

# Method 2: From a downloaded APK
if [ -n "$1" ] && [ -f "$1" ]; then
  echo "📱 Extracting signing cert from APK: $1"
  # Use apksigner to get the signing certificate
  if command -v apksigner &> /dev/null; then
    SHA256_HEX=$(apksigner verify --print-certs "$1" 2>/dev/null \
      | grep "Signer #1 certificate SHA-256" \
      | awk '{print $NF}' \
      | tr -d ':' \
      | tr '[:upper:]' '[:lower:]')
    
    if [ -n "$SHA256_HEX" ]; then
      CHECKSUM=$(echo "$SHA256_HEX" | xxd -r -p | base64 | tr '+/' '-_' | tr -d '=')
      echo "✅ APK signing certificate checksum (URL-safe Base64):"
      echo "   $CHECKSUM"
      echo ""
      echo "Updating checksum.json..."
      cat > emi-admin-dashboard/src/checksum.json <<EOF
{
  "signatureChecksum": "$CHECKSUM"
}
EOF
      echo "✅ Updated emi-admin-dashboard/src/checksum.json"
    else
      echo "❌ Could not extract SHA-256 from APK"
    fi
  else
    echo "❌ apksigner not found. Install Android SDK Build Tools."
    echo "   Or use: ./extract_checksum.sh --play-hex '<SHA256_FROM_PLAY_CONSOLE>'"
  fi
  exit 0
fi

# Method 3: From local keystore (default)
KEYSTORE_PATH="EmiLockerApp/app/release.keystore"
KEY_ALIAS="emilocker"
STORE_PASSWORD="password"

echo "🔑 Extracting from local keystore: $KEYSTORE_PATH"

if [ ! -f "$KEYSTORE_PATH" ]; then
  echo "❌ Keystore not found at $KEYSTORE_PATH"
  echo ""
  echo "Alternative methods:"
  echo "  1. Get SHA-256 from Play Console → Setup → App signing"
  echo "     Then run: ./extract_checksum.sh --play-hex 'AB:CD:EF:...'"
  echo ""
  echo "  2. Download Google-signed APK from Play Console → App Bundle Explorer"
  echo "     Then run: ./extract_checksum.sh /path/to/downloaded.apk"
  exit 1
fi

SHA256_HEX=$(keytool -list -v -keystore "$KEYSTORE_PATH" -alias "$KEY_ALIAS" -storepass "$STORE_PASSWORD" 2>/dev/null \
  | grep "SHA256:" \
  | sed 's/.*SHA256: //' \
  | tr -d ':' \
  | tr '[:upper:]' '[:lower:]')

if [ -n "$SHA256_HEX" ]; then
  CHECKSUM=$(echo "$SHA256_HEX" | xxd -r -p | base64 | tr '+/' '-_' | tr -d '=')
  echo "✅ Local keystore signing certificate checksum (URL-safe Base64):"
  echo "   $CHECKSUM"
  echo ""
  echo "Updating checksum.json..."
  cat > emi-admin-dashboard/src/checksum.json <<EOF
{
  "signatureChecksum": "$CHECKSUM"
}
EOF
  echo "✅ Updated emi-admin-dashboard/src/checksum.json"
  echo ""
  echo "⚠️  NOTE: If you use Google Play App Signing, the Play Store may have"
  echo "   re-signed your app with a DIFFERENT key. In that case, get the SHA-256"
  echo "   from Play Console → Setup → App signing → App signing key certificate"
  echo "   and run:"
  echo "   ./extract_checksum.sh --play-hex '<SHA256_FROM_PLAY_CONSOLE>'"
else
  echo "❌ Could not extract SHA-256 from keystore"
fi
