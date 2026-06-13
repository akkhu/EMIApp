import { VercelRequest, VercelResponse } from '@vercel/node';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { username, password } = req.body;

  const validUsername = process.env.ADMIN_USERNAME;
  const validPassword = process.env.ADMIN_PASSWORD;

  if (!validUsername || !validPassword) {
    return res.status(500).json({ error: 'Server configuration error: Credentials not set' });
  }

  if (username === validUsername && password === validPassword) {
    // Return a dummy token
    return res.status(200).json({ 
      success: true, 
      token: 'mvp-admin-token-778899',
      message: 'Login successful' 
    });
  }

  return res.status(401).json({ message: 'Invalid username or password' });
}
