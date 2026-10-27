import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDevice, saveDevice } from './lib/store.js';
import { pushAction } from './lib/fcm.js';

// Marks the device released (kept, not deleted) so the phone also releases itself
// on its next status poll even if the instant push doesn't arrive.
export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { deviceId } = req.body || {};
  if (!deviceId) return res.status(400).json({ error: 'deviceId is required' });
  try {
    const d = await getDevice(deviceId);
    if (!d) return res.status(404).json({ error: 'Device not found' });
    d.isLocked = false;
    d.released = true;
    await saveDevice(d);
    const pushed = await pushAction(d.fcmToken, 'RELEASE');
    return res.status(200).json({ success: true, pushed });
  } catch (e: any) {
    console.error('release error', e);
    return res.status(503).json({ error: String(e?.message || e) });
  }
}
