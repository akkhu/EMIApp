import { VercelRequest, VercelResponse } from '@vercel/node';
import { addLog } from './lib/store.js';

// QR download entry point: logs the request, then redirects to the static APK.
export default async function handler(req: VercelRequest, res: VercelResponse) {
  const doc = {
    stage: 'apk_download',
    detail: `ua=${req.headers['user-agent'] || ''} range=${req.headers['range'] || ''} method=${req.method}`,
    model: '', sdk: 0, version: '',
    at: new Date().toISOString(),
  };
  console.log('PROVLOG', JSON.stringify(doc));
  try { await addLog(doc); } catch (e) { console.warn('apk log failed', e); }
  res.setHeader('Cache-Control', 'no-store');
  res.redirect(302, 'https://emi-app-7pq1.vercel.app/emilocker.apk');
}
