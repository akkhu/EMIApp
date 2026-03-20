import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { LogOut, Smartphone, Lock, Unlock, ShieldAlert, QrCode, X } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';

const API_URL = import.meta.env.VITE_API_URL || 'http://192.168.1.8:5031/api';

interface Device {
  appId: string;
  imei: string;
  customerName: string;
  lockStatus: string;
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedDeviceQr, setSelectedDeviceQr] = useState<Device | null>(null);

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

  const handleLockAction = async (appId: string, imei: string, action: 'lock' | 'unlock') => {
    try {
      const token = localStorage.getItem('token');
      await axios.post(`${API_URL}/device/${appId}/${imei}/${action}`, {}, {
        headers: { Authorization: `Bearer ${token}` }
      });
      fetchDevices(); // refresh the list
    } catch (err) {
      console.error(err);
      alert('Failed to update device status.');
    }
  };

  const getQRPayload = (device: Device) => {
    return JSON.stringify({
      action: "REGISTER_AND_LOCK",
      appId: device.appId,
      imei: device.imei,
      serverUrl: API_URL
    });
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
          <div className="p-6 border-b border-white/10 bg-black/20">
            <h2 className="text-xl font-semibold flex items-center space-x-2">
              <Smartphone className="text-gray-400" />
              <span>Registered Devices</span>
            </h2>
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-black/30 text-gray-400 text-sm uppercase tracking-wider">
                  <th className="p-4 rounded-tl-lg">App ID</th>
                  <th className="p-4">Customer</th>
                  <th className="p-4">IMEI</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 rounded-tr-lg">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {loading ? (
                  <tr><td colSpan={5} className="p-8 text-center text-gray-500">Loading devices...</td></tr>
                ) : devices.map((dev) => (
                  <tr key={dev.imei} className="hover:bg-white/5 transition-colors">
                    <td className="p-4 text-xs font-mono font-bold text-blue-400">{dev.appId || 'DEFAULT'}</td>
                    <td className="p-4 font-medium">{dev.customerName}</td>
                    <td className="p-4 text-gray-400 font-mono text-sm">{dev.imei}</td>
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
                          onClick={() => handleLockAction(dev.appId || 'DEFAULT_APP', dev.imei, 'unlock')}
                          className="flex items-center justify-center w-8 h-8 bg-green-600/20 hover:bg-green-600/30 text-green-400 rounded border border-green-500/20 transition-colors tooltip-trigger relative group">
                          <Unlock size={14} />
                          <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity">Unlock</span>
                        </button>
                      ) : (
                        <button 
                          onClick={() => handleLockAction(dev.appId || 'DEFAULT_APP', dev.imei, 'lock')}
                          className="flex items-center justify-center w-8 h-8 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded border border-red-500/20 transition-colors tooltip-trigger relative group">
                          <Lock size={14} />
                          <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity">Lock</span>
                        </button>
                      )}
                      <button 
                        onClick={() => setSelectedDeviceQr(dev)}
                        className="flex items-center justify-center w-8 h-8 bg-blue-600/20 hover:bg-blue-600/30 text-blue-400 rounded border border-blue-500/20 transition-colors relative group">
                        <QrCode size={14} />
                        <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">QR Code</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* QR Code Modal Overlay */}
      {selectedDeviceQr && (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-gray-900 border border-white/10 p-6 rounded-2xl shadow-2xl max-w-sm w-full animate-in fade-in zoom-in duration-200">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-bold">Lock Device via QR</h3>
              <button 
                onClick={() => setSelectedDeviceQr(null)}
                className="p-1 hover:bg-white/10 rounded-full transition-colors text-gray-400 hover:text-white">
                <X size={20} />
              </button>
            </div>
            
            <div className="bg-white p-4 rounded-xl flex justify-center items-center">
              <QRCodeSVG 
                value={getQRPayload(selectedDeviceQr)} 
                size={200}
                level="H"
                includeMargin={false}
              />
            </div>
            
            <div className="mt-6 text-center">
              <p className="text-sm text-gray-400 mb-2">Scan from Emilocker App</p>
              <div className="bg-black/30 p-3 rounded-lg border border-white/5">
                <p className="text-xs font-mono text-blue-300">IMEI: {selectedDeviceQr.imei}</p>
                <p className="text-xs font-mono text-purple-300 mt-1">APP: {selectedDeviceQr.appId || 'DEFAULT'}</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
