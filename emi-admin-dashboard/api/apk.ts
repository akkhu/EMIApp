import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDb } from './lib/mongodb.js';

// QR download entry point: records that a phone requested the APK, then
// redirects to the static file. Shows up in /api/provlog as stage "apk_download".
export default async function handler(req: VercelRequest, res: VercelResponse) {
  const doc = {
    stage: 'apk_download',
    detail: `ua=${req.headers['user-agent'] || ''} range=${req.headers['range'] || ''} method=${req.method}`,
    model: '', sdk: 0, version: '',
    ip: String(req.headers['x-forwarded-for'] || ''),
    at: new Date().toISOString(),
  };
  console.log('PROVLOG', JSON.stringify(doc));
  try {
    const db = await getDb();
    await db.collection('provlogs').insertOne(doc);
  } catch (e) {
    console.warn('apk log failed', e);
  }
  res.setHeader('Cache-Control', 'no-store');
  res.redirect(302, 'https://emi-app-7pq1.vercel.app/emilocker.apk');
}
