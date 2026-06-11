import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDb } from './lib/mongodb.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { deviceId } = req.query;

    if (!deviceId) {
      return res.status(400).json({ error: 'deviceId is required' });
    }

    const db = await getDb();
    const collection = db.collection('devices');

    const device = await collection.findOne({ deviceId });

    if (!device) {
      return res.status(404).json({ error: 'Device not found' });
    }

    // Update lastSeen
    await collection.updateOne({ deviceId }, { $set: { lastSeen: new Date() } });

    return res.status(200).json({ isLocked: device.isLocked });
  } catch (error) {
    console.error('Status check error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
