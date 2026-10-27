// Storage on Vercel: Upstash Redis (Vercel dashboard -> Storage -> Upstash for Redis).
// Vercel injects KV_REST_API_URL / KV_REST_API_TOKEN when the store is connected.
const URL = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
const TOKEN = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;

export async function redis(...cmd: (string | number)[]): Promise<any> {
  if (!URL || !TOKEN) {
    throw new Error('Storage not connected: create an Upstash Redis store in Vercel (Storage tab) and connect it to this project');
  }
  const r = await fetch(URL, {
    method: 'POST',
    headers: { Authorization: `Bearer ${TOKEN}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(cmd),
  });
  const j: any = await r.json();
  if (j.error) throw new Error(j.error);
  return j.result;
}

export interface Device {
  deviceId: string;
  userName: string;
  phoneNumber: string;
  fcmToken: string;
  isLocked: boolean;
  released?: boolean;
  registeredAt: string;
  lastSeen: string;
}

export async function getDevice(id: string): Promise<Device | null> {
  const v = await redis('GET', `device:${id}`);
  return v ? JSON.parse(v) : null;
}

export async function saveDevice(d: Device): Promise<void> {
  await redis('SET', `device:${d.deviceId}`, JSON.stringify(d));
  await redis('SADD', 'devices', d.deviceId);
}

export async function listDevices(): Promise<Device[]> {
  const ids: string[] = (await redis('SMEMBERS', 'devices')) || [];
  if (!ids.length) return [];
  const vals: (string | null)[] = await redis('MGET', ...ids.map((i) => `device:${i}`));
  return vals.filter(Boolean).map((v) => JSON.parse(v as string));
}

export async function addLog(doc: Record<string, unknown>): Promise<void> {
  await redis('LPUSH', 'provlogs', JSON.stringify(doc));
  await redis('LTRIM', 'provlogs', 0, 199);
}

export async function listLogs(n = 50): Promise<any[]> {
  const v: string[] = (await redis('LRANGE', 'provlogs', 0, n - 1)) || [];
  return v.map((x) => JSON.parse(x));
}
