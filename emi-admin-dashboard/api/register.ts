import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDevice, saveDevice, Device } from './lib/store.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') return res.status(405).json({ error: 'Method not allowed' });
  const { deviceId, userName, phoneNumber, fcmToken } = req.body || {};
  if (!deviceId) return res.status(400).json({ error: 'deviceId is required' });
  try {
    const now = new Date().toISOString();
    const existing = await getDevice(deviceId);
    const d: Device = {
      deviceId,
      userName: userName || existing?.userName || 'Android device',
      phoneNumber: phoneNumber || existing?.phoneNumber || '',
      fcmToken: fcmToken || existing?.fcmToken || '',
      isLocked: existing?.isLocked ?? false,
      released: existing?.released ?? false,
      registeredAt: existing?.registeredAt || now,
      lastSeen: now,
    };
    await saveDevice(d);
    return res.status(200).json({ success: true, device: d });
  } catch (e: any) {
    console.error('register error', e);
    return res.status(503).json({ error: String(e?.message || e) });
  }
}
