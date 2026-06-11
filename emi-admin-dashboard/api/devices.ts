import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDb } from './lib/mongodb.js';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  try {
    const db = await getDb();
    const collection = db.collection('devices');

    const devices = await collection.find({}).sort({ registeredAt: -1 }).toArray();

    return res.status(200).json({ devices });
  } catch (error) {
    console.error('Get devices error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
