import { VercelRequest, VercelResponse } from '@vercel/node';
import { addLog, listLogs } from './lib/store.js';

// Provisioning diagnostics. Phones POST each setup step they reach;
// GET /api/provlog shows the latest 50 events (newest first).
export default async function handler(req: VercelRequest, res: VercelResponse) {
  try {
    if (req.method === 'POST') {
      const b = req.body || {};
      const doc = {
        stage: String(b.stage || 'unknown'),
        detail: String(b.detail || ''),
        model: String(b.model || ''),
        sdk: Number(b.sdk || 0),
        version: String(b.version || ''),
        at: new Date().toISOString(),
      };
      console.log('PROVLOG', JSON.stringify(doc));
      await addLog(doc);
      return res.status(200).json({ ok: true });
    }
    return res.status(200).json({ events: await listLogs(50) });
  } catch (e: any) {
    console.error('provlog error', e);
    return res.status(200).json({ ok: false, error: String(e?.message || e) });
  }
}
