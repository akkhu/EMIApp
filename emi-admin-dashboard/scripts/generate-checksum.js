import fs from 'fs';
import crypto from 'crypto';
import path from 'path';

const publicDir = './public';
const versionData = JSON.parse(fs.readFileSync('./src/version.json', 'utf8'));
const apkFileName = versionData.apkFileName;
const apkPath = apkFileName ? path.join(publicDir, apkFileName) : null;

// Local release.keystore signature checksum
// (Correct checksum for emilocker-release-key.jks)
const SIGNATURE_CHECKSUM = 'gRYpPvtWmcBzpDb2nf-tW0d5XJvxIcC3Xplgnk_dbqM';

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
