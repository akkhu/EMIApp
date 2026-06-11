import { VercelRequest, VercelResponse } from '@vercel/node';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method not allowed' });
  }

  const { username, password } = req.body;

  // Hardcoded MVP Admin Credentials
  if (username === 'admin' && password === 'password123') {
    // Return a dummy token
    return res.status(200).json({ 
      success: true, 
      token: 'mvp-admin-token-778899',
      message: 'Login successful' 
    });
  }

  return res.status(401).json({ message: 'Invalid username or password' });
}
