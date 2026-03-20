import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { LogOut, Smartphone, Lock, Unlock, ShieldAlert } from 'lucide-react';

const API_URL = import.meta.env.VITE_API_URL || 'http://192.168.1.8:5031/api';

interface Device {
  appId: string;
  imei: string;
  customerName: string;
  dob: string;
  aadhaarNumber: string;
  panNumber: string;
  fatherName: string;
  village: string;
  clientName: string;
  lockStatus: string;
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [clientFilter, setClientFilter] = useState<string>('All');

  const fetchDevices = async () => {
    try {
      const token = localStorage.getItem('token');
      const res = await axios.get(`${API_URL}/device`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setDevices(res.data);
    } catch (err: any) {
      if (err.response?.status === 401) handleLogout();
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDevices();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const handleLockAction = async (appId: string, aadhaarNumber: string, action: 'lock' | 'unlock') => {
    try {
      const token = localStorage.getItem('token');
      await axios.post(`${API_URL}/device/${appId}/${aadhaarNumber}/${action}`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      fetchDevices(); // refresh the list
    } catch (err) {
      console.error(err);
      alert('Failed to update device status.');
    }
  };

  return (
    <div className="min-h-screen p-8 relative">
      <div className="max-w-6xl mx-auto space-y-8">
        
        {/* Header */}
        <div className="flex justify-between items-center glass p-6">
          <div className="flex items-center space-x-4">
            <div className="p-3 bg-blue-500/20 rounded-full">
              <ShieldAlert className="text-blue-400" size={32} />
            </div>
            <div>
              <h1 className="text-2xl font-bold tracking-wider">EMI ADMIN PORTAL</h1>
              <p className="text-gray-400 text-sm">Manage Device Financing & Security</p>
            </div>
          </div>
          <button onClick={handleLogout} className="flex items-center space-x-2 px-4 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg transition-colors border border-red-500/20 shadow-lg">
            <LogOut size={18} />
            <span>Logout</span>
          </button>
        </div>

        {/* Dashboard Grid */}
        <div className="glass overflow-hidden">
          <div className="p-6 border-b border-white/10 bg-black/20 flex justify-between items-center">
            <h2 className="text-xl font-semibold flex items-center space-x-2">
              <Smartphone className="text-gray-400" />
              <span>Registered Devices</span>
            </h2>
            
            {/* Client Filter Dropdown */}
            <div className="flex items-center space-x-3">
              <label htmlFor="clientFilter" className="text-sm text-gray-400 font-medium">Filter by Client:</label>
              <select 
                id="clientFilter"
                value={clientFilter}
                onChange={(e) => setClientFilter(e.target.value)}
                className="bg-black/40 border border-white/10 rounded-lg px-4 py-2 text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500/50 appearance-none"
              >
                <option value="All">All Clients</option>
                <option value="test1">test1</option>
                <option value="test2">test2</option>
                <option value="test3">test3</option>
              </select>
            </div>
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-black/30 text-gray-400 text-sm uppercase tracking-wider">
                  <th className="p-4 rounded-tl-lg">App ID</th>
                  <th className="p-4">Customer Details</th>
                  <th className="p-4">Location &amp; Client</th>
                  <th className="p-4">IMEI</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 rounded-tr-lg">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {loading ? (
                  <tr><td colSpan={6} className="p-8 text-center text-gray-500">Loading devices...</td></tr>
                ) : devices
                      .filter(dev => clientFilter === 'All' || dev.clientName === clientFilter)
                      .map((dev) => (
                  <tr key={dev.aadhaarNumber} className="hover:bg-white/5 transition-colors">
                    <td className="p-4 text-xs font-mono font-bold text-blue-400">{dev.appId || 'DEFAULT'}</td>
                    <td className="p-4">
                      <div className="font-medium">{dev.customerName || 'N/A'}</div>
                      <div className="text-xs text-gray-400 mt-1">S/O: {dev.fatherName || '-'} | DOB: {dev.dob || '-'}</div>
                      <div className="text-xs text-gray-500 mt-0.5">Aadhaar: {dev.aadhaarNumber || '-'} | PAN: {dev.panNumber || '-'}</div>
                    </td>
                    <td className="p-4">
                      <div className="font-medium text-sm">{dev.village || 'N/A'}</div>
                      <div className="text-xs font-mono text-purple-400 mt-1 border border-purple-500/30 bg-purple-500/10 inline-block px-2 py-0.5 rounded">Client: {dev.clientName || 'None'}</div>
                    </td>
                    <td className="p-4 text-gray-400 font-mono text-sm">{dev.aadhaarNumber}</td>
                    <td className="p-4">
                      <span className={`px-3 py-1 rounded-full text-xs font-medium border ${
                        dev.lockStatus === 'Locked' 
                          ? 'bg-red-500/10 text-red-400 border-red-500/20' 
                          : 'bg-green-500/10 text-green-400 border-green-500/20'
                      }`}>
                        {dev.lockStatus}
                      </span>
                    </td>
                    <td className="p-4 flex space-x-2">
                      {dev.lockStatus === 'Locked' ? (
                        <button 
                          onClick={() => handleLockAction(dev.appId || 'DEFAULT_APP', dev.aadhaarNumber, 'unlock')}
                          className="flex items-center justify-center w-8 h-8 bg-green-600/20 hover:bg-green-600/30 text-green-400 rounded border border-green-500/20 transition-colors tooltip-trigger relative group">
                          <Unlock size={14} />
                          <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity">Unlock</span>
                        </button>
                      ) : (
                        <button 
                          onClick={() => handleLockAction(dev.appId || 'DEFAULT_APP', dev.aadhaarNumber, 'lock')}
                          className="flex items-center justify-center w-8 h-8 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded border border-red-500/20 transition-colors tooltip-trigger relative group">
                          <Lock size={14} />
                          <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity">Lock</span>
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
