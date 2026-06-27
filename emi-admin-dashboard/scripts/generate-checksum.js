import fs from 'fs';
import crypto from 'crypto';
import path from 'path';

const publicDir = './public';
const apkFiles = fs.readdirSync(publicDir).filter(file => file.endsWith('.apk'));
const apkPath = apkFiles.length > 0 ? path.join(publicDir, apkFiles[0]) : null;

// Google Play App Signing certificate SHA-256 digest (constant across builds)
// This only changes if Google rotates the signing key in Play Console
const GOOGLE_SIGNING_CERT_CHECKSUM = '0OcWY9LK7lYVeQCknepeJ36LCrPeGuDJeAIv5l_Jq1I';

try {
  if (apkPath && fs.existsSync(apkPath)) {
    const fileBuffer = fs.readFileSync(apkPath);
    const hashSum = crypto.createHash('sha256');
    hashSum.update(fileBuffer);
    
    // Android requires Base64 URL-safe encoding without padding
    const packageChecksum = hashSum.digest('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
      
    const checksumData = {
      packageChecksum,
      signatureChecksum: GOOGLE_SIGNING_CERT_CHECKSUM
    };
    
    fs.writeFileSync('./src/checksum.json', JSON.stringify(checksumData, null, 2));
    console.log('Successfully generated checksums for EmiLockerApp.apk');
    console.log('  Package checksum:', packageChecksum);
    console.log('  Signature checksum:', GOOGLE_SIGNING_CERT_CHECKSUM);
  } else {
    console.warn('EmiLockerApp.apk not found in public folder. Generating empty checksums.');
    fs.writeFileSync('./src/checksum.json', JSON.stringify({ packageChecksum: '', signatureChecksum: '' }, null, 2));
  }
} catch (error) {
  console.error('Failed to generate checksum:', error);
  process.exit(1);
}
