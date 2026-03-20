import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { ShieldCheck, User, Lock, Key } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://192.168.1.8:5031/api'; // Live server fallback to local

export default function Login() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('password123');
  const [otp, setOtp] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      // In a real app we would get the port dynamically or from .env
      const res = await axios.post(`${API_URL}/auth/login`, { username, password });
      if (res.data.requiresOtp) {
        console.log("MOCK OTP IS: ", res.data.mockOtp);
        setStep(2);
        setError('');
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Login failed or Server Offline');
    }
  };

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await axios.post(`${API_URL}/auth/verify-otp`, { username, otpCode: otp });
      localStorage.setItem('token', res.data.token);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Invalid OTP');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="glass p-8 w-full max-w-md transition-all duration-500 relative overflow-hidden">
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 to-purple-500" />
        
        <div className="flex flex-col items-center mb-8">
          <div className="p-3 bg-blue-500/20 rounded-full mb-4">
            <ShieldCheck size={40} className="text-blue-400" />
          </div>
          <h2 className="text-2xl font-bold tracking-wider">EMI SECURITY</h2>
          <p className="text-gray-400 text-sm mt-1">Authorized Personnel Only</p>
        </div>

        {error && (
          <div className="bg-red-500/20 border border-red-500/50 text-red-200 p-3 rounded-lg mb-6 text-sm text-center font-medium">
            {error}
          </div>
        )}

        {step === 1 ? (
          <form onSubmit={handleLogin} className="space-y-5">
            <div className="relative">
              <User className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="text"
                placeholder="Username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full bg-black/20 border border-white/10 rounded-lg py-3 pl-10 pr-4 focus:outline-none focus:ring-2 focus:ring-blue-500 text-white placeholder-gray-500 transition-all font-medium"
                required
              />
            </div>
            
            <div className="relative">
              <Lock className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-black/20 border border-white/10 rounded-lg py-3 pl-10 pr-4 focus:outline-none focus:ring-2 focus:ring-blue-500 text-white placeholder-gray-500 transition-all font-medium"
                required
              />
            </div>

            <button type="submit" className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-500 hover:to-purple-500 text-white font-semibold py-3 rounded-lg shadow-lg hover:shadow-blue-500/25 transition-all transform hover:-translate-y-0.5 mt-4">
              Send OTP
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerify} className="space-y-5">
            <div className="text-center mb-6">
              <p className="text-sm text-blue-200">A 6-digit code has been printed in the C# terminal (Mock SMS).</p>
            </div>
            
            <div className="relative">
              <Key className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" size={20} />
              <input
                type="text"
                placeholder="Enter 6-digit OTP"
                value={otp}
                onChange={(e) => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                className="w-full bg-black/20 border border-blue-500/50 rounded-lg py-3 pl-10 pr-4 focus:outline-none focus:ring-2 focus:ring-purple-500 text-white text-center tracking-widest text-lg font-mono transition-all"
                required
              />
            </div>

            <button type="submit" className="w-full bg-gradient-to-r from-green-500 to-emerald-600 hover:from-green-400 hover:to-emerald-500 text-white font-semibold py-3 rounded-lg shadow-lg hover:shadow-green-500/25 transition-all transform hover:-translate-y-0.5 mt-4">
              Verify & Enter
            </button>
            
            <button type="button" onClick={() => setStep(1)} className="w-full text-gray-400 hover:text-white text-sm transition-colors mt-4">
              Back to Login
            </button>
          </form>
        )}
      </div>
    </div>
  );
}
