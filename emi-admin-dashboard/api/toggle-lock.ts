import { VercelRequest, VercelResponse } from '@vercel/node';
import { getDb } from './lib/mongodb.js';
import { getApps, initializeApp, cert } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

// Initialize Firebase Admin once
if (!getApps().length) {
  try {
    const serviceAccountStr = process.env.FIREBASE_SERVICE_ACCOUNT;
    if (serviceAccountStr) {
      const serviceAccount = JSON.parse(serviceAccountStr);
      initializeApp({
        credential: cert(serviceAccount)
      });
    }
  } catch (error) {
    console.error('Firebase admin init error:', error);
  }
}

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

    // Fetch device to get FCM token
    const device = await collection.findOne({ deviceId });
    if (!device) {
      return res.status(404).json({ error: 'Device not found' });
    }

    const result = await collection.updateOne(
      { deviceId },
      { $set: { isLocked } }
    );

    // Send FCM push notification
    if (device.fcmToken && getApps().length > 0) {
      try {
        await getMessaging().send({
          token: device.fcmToken,
          data: {
            action: isLocked ? 'LOCK' : 'UNLOCK'
          }
        });
        console.log(`FCM sent successfully to ${deviceId}`);
      } catch (fcmError) {
        console.error('FCM send error:', fcmError);
      }
    }

    return res.status(200).json({ success: true, isLocked });
  } catch (error) {
    console.error('Toggle lock error:', error);
    return res.status(500).json({ error: 'Internal server error' });
  }
}
