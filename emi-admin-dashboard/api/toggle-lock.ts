import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDevice, saveDevice } from './lib/store.js';
import { pushAction } from './lib/fcm.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { deviceId, isLocked } = req.body || {};
  if (!deviceId || typeof isLocked !== 'boolean') {
    return res.status(400).json({ error: 'deviceId and boolean isLocked are required' });
  }
  try {
    const d = await getDevice(deviceId);
    if (!d) return res.status(404).json({ error: 'Device not found' });
    d.isLocked = isLocked;
    await saveDevice(d);
    const pushed = await pushAction(d.fcmToken, isLocked ? 'LOCK' : 'UNLOCK');
    return res.status(200).json({ success: true, isLocked, pushed });
  } catch (e: any) {
    console.error('toggle-lock error', e);
    return res.status(503).json({ error: String(e?.message || e) });
  }
}
