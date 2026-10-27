import { MongoClient, Db } from 'mongodb';

let client: MongoClient | null = null;
let clientPromise: Promise<MongoClient> | null = null;

export async function getDb(): Promise<Db> {
  const uri = process.env.MONGODB_URI;
  if (!uri) {
    throw new Error('MONGODB_URI is not defined in environment variables');
  }

  if (!clientPromise) {
    const options = {
      serverSelectionTimeoutMS: 4000,
      connectTimeoutMS: 4000,
    };
    client = new MongoClient(uri, options);
    clientPromise = client.connect();
  }

  const connectedClient = await clientPromise;
  return connectedClient.db('emiLocker');
}

export default clientPromise;

