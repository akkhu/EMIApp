import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDb } from './lib/mongodb.js';

// Provisioning diagnostics. Phones POST each setup step they reach;
// GET /api/provlog shows the most recent 50 events (newest first).
export default async function handler(req: VercelRequest, res: VercelResponse) {
  try {
    const db = await getDb();
    const col = db.collection('provlogs');
    if (req.method === 'POST') {
      const b = req.body || {};
      const doc = {
        stage: String(b.stage || 'unknown'),
        detail: String(b.detail || ''),
        model: String(b.model || ''),
        sdk: Number(b.sdk || 0),
        version: String(b.version || ''),
        ip: String(req.headers['x-forwarded-for'] || ''),
        at: new Date().toISOString(),
      };
      console.log('PROVLOG', JSON.stringify(doc));
      await col.insertOne(doc);
      return res.status(200).json({ ok: true });
    }
    const rows = await col.find({}, { projection: { _id: 0 } }).sort({ at: -1 }).limit(50).toArray();
    return res.status(200).json({ events: rows });
  } catch (e: any) {
    console.error('provlog error', e);
    return res.status(200).json({ ok: false, error: String(e?.message || e) });
  }
}
