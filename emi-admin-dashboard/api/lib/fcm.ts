import { getApps, initializeApp, cert } from 'firebase-admin/app';
import { getMessaging } from 'firebase-admin/messaging';

if (!getApps().length && process.env.FIREBASE_SERVICE_ACCOUNT) {
  try {
    initializeApp({ credential: cert(JSON.parse(process.env.FIREBASE_SERVICE_ACCOUNT)) });
  } catch (e) {
    console.error('Firebase admin init error:', e);
  }
}

// Instant push to the phone. Optional: if Firebase isn't configured, phones still
// pick up the change on their next status poll.
export async function pushAction(token: string | undefined, action: 'LOCK' | 'UNLOCK' | 'RELEASE') {
  if (!token || !getApps().length) return false;
  try {
    await getMessaging().send({ token, data: { action } });
    return true;
  } catch (e) {
    console.error('FCM send error:', e);
    return false;
  }
}
