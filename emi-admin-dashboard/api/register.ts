import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDb } from './lib/mongodb.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const { deviceId, userName, phoneNumber } = req.body;

    if (!deviceId) {
      return res.status(400).json({ error: 'deviceId is required' });
    }

    const db = await getDb();
    const collection = db.collection('devices');

    const newDevice = {
      deviceId,
      userName,
      phoneNumber,
      isLocked: false,
      registeredAt: new Date(),
      lastSeen: new Date(),
    };

    // Upsert the device so we don't duplicate on re-registration
    await collection.updateOne(
      { deviceId },
      { $set: newDevice },
      { upsert: true }
    );

    return res.status(200).json({ success: true, device: newDevice });
  } catch (error) {
    console.error('Registration error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
