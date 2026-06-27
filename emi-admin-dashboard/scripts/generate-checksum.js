import fs from 'fs';
import crypto from 'crypto';
import path from 'path';

const publicDir = './public';
const versionData = JSON.parse(fs.readFileSync('./src/version.json', 'utf8'));
const apkFileName = versionData.apkFileName;
const apkPath = apkFileName ? path.join(publicDir, apkFileName) : null;

// Google Play App Signing certificate SHA-256 digest
const SIGNATURE_CHECKSUM = '0OcWY9LK7lYVeQCknepeJ36LCrPeGuDJeAIv5l_Jq1I';

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
      signatureChecksum: SIGNATURE_CHECKSUM
    };
    
    fs.writeFileSync('./src/checksum.json', JSON.stringify(checksumData, null, 2));
    console.log(`Successfully generated checksums for ${apkFileName}`);
    console.log('  Package checksum:', packageChecksum);
    console.log('  Signature checksum:', SIGNATURE_CHECKSUM);
  } else {
    console.warn(`${apkFileName || 'APK'} not found in public folder. Keeping Play signature checksum only.`);
    fs.writeFileSync('./src/checksum.json', JSON.stringify({
      packageChecksum: '',
      signatureChecksum: SIGNATURE_CHECKSUM
    }, null, 2));
  }
} catch (error) {
  console.error('Failed to generate checksum:', error);
  process.exit(1);
}
