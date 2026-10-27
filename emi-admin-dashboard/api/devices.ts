import { VercelRequest, VercelResponse } from '@vercel/node';
import { listDevices } from './lib/store.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).json({ error: 'Method not allowed' });
  try {
    const devices = (await listDevices())
      .filter((d) => !d.released)
      .sort((a, b) => (b.registeredAt || '').localeCompare(a.registeredAt || ''));
    return res.status(200).json({ devices });
  } catch (e: any) {
    console.error('devices error', e);
    return res.status(503).json({ error: String(e?.message || e), devices: [] });
  }
}
