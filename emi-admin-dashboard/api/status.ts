import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDevice, saveDevice } from './lib/store.js';

// Polled by phones. Returns { isLocked, released }. On storage errors returns 503
// so phones keep their current state instead of unlocking.
export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).json({ error: 'Method not allowed' });
  const deviceId = String(req.query.deviceId || '');
  if (!deviceId) return res.status(400).json({ error: 'deviceId is required' });
  try {
    const d = await getDevice(deviceId);
    if (!d) return res.status(200).json({ isLocked: false, released: false });
    d.lastSeen = new Date().toISOString();
    await saveDevice(d);
    return res.status(200).json({ isLocked: Boolean(d.isLocked), released: Boolean(d.released) });
  } catch (e: any) {
    console.error('status error', e);
    return res.status(503).json({ error: String(e?.message || e) });
  }
}
