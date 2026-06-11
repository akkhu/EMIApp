import fs from 'fs';
import crypto from 'crypto';

const apkPath = './public/EmiLockerApp.apk';

try {
  if (fs.existsSync(apkPath)) {
    const fileBuffer = fs.readFileSync(apkPath);
    const hashSum = crypto.createHash('sha256');
    hashSum.update(fileBuffer);
    
    // Android requires Base64 URL-safe encoding without padding
    const checksum = hashSum.digest('base64')
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/, '');
      
    fs.writeFileSync('./src/checksum.json', JSON.stringify({ checksum }, null, 2));
    console.log('Successfully generated checksum for EmiLockerApp.apk:', checksum);
  } else {
    console.warn('EmiLockerApp.apk not found in public folder. Generating empty checksum.');
    fs.writeFileSync('./src/checksum.json', JSON.stringify({ checksum: '' }, null, 2));
  }
} catch (error) {
  console.error('Failed to generate checksum:', error);
  process.exit(1);
}
