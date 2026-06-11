import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDb } from './lib/mongodb.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { deviceId, isLocked } = req.body;

    if (!deviceId || typeof isLocked !== 'boolean') {
      return res.status(400).json({ error: 'deviceId and boolean isLocked are required' });
    }

    const db = await getDb();
    const collection = db.collection('devices');

    const result = await collection.updateOne(
      { deviceId },
      { $set: { isLocked } }
    );

    if (result.matchedCount === 0) {
      return res.status(404).json({ error: 'Device not found' });
    }

    return res.status(200).json({ success: true, isLocked });
  } catch (error) {
    console.error('Toggle lock error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
