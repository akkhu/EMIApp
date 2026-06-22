import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import { LogOut, Smartphone, Lock, Unlock, ShieldAlert, QrCode, X, Trash2 } from 'lucide-react';
import { QRCodeSVG } from 'qrcode.react';
import checksumData from '../checksum.json';
import versionData from '../version.json';

const API_URL = '/api'; // Using Vercel serverless functions

interface Device {
  deviceId: string;
  userName: string;
  phoneNumber: string;
  isLocked: boolean;
  registeredAt: string;
  lastSeen: string;
}

export default function Dashboard() {
  const navigate = useNavigate();
  const [devices, setDevices] = useState<Device[]>([]);
  const [loading, setLoading] = useState(true);
  const [showOnboardingQr, setShowOnboardingQr] = useState(false);

  const fetchDevices = async () => {
    try {
      const res = await axios.get(`${API_URL}/devices`);
      setDevices(res.data.devices || []);
    } catch (err: unknown) {
      console.error('Failed to fetch devices', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDevices();
    const interval = setInterval(fetchDevices, 10000); // Auto-refresh every 10s
    return () => clearInterval(interval);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    navigate('/login');
  };

  const handleLockAction = async (deviceId: string, isLocked: boolean) => {
    try {
      await axios.post(`${API_URL}/toggle-lock`, { deviceId, isLocked });
      fetchDevices(); // refresh the list
    } catch (err) {
      console.error(err);
      alert('Failed to update device status.');
    }
  };

  const handleReleaseDevice = async (deviceId: string) => {
    if (!window.confirm("Are you sure you want to release this device? This will permanently remove the EMI lock, unhide the app, and allow the customer to uninstall it. You will not be able to lock this device again unless you factory reset it!")) {
      return;
    }
    
    try {
      await axios.post(`${API_URL}/release-device`, { deviceId });
      fetchDevices(); // refresh the list
    } catch (err) {
      console.error(err);
      alert('Failed to release device.');
    }
  };

  const getOnboardingUrl = () => {
    // Android Device Owner Provisioning QR Code Payload
    const payload = {
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": "com.scmemilocker.app/com.scmemilocker.app.receivers.EmiDeviceAdminReceiver",
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": `${window.location.origin}/EmiLockerApp.apk`,
      "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM": checksumData.packageChecksum,
      "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": true,
      "android.app.extra.PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED": true,
      "android.app.extra.PROVISIONING_SKIP_EDUCATION_SCREENS": true,
      "android.app.extra.PROVISIONING_USE_MOBILE_DATA": true
    };
    return JSON.stringify(payload);
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
            <div className="ml-4 px-3 py-1 bg-white/5 rounded-full border border-white/10 hidden sm:block">
              <span className="text-xs text-gray-400">APK v{versionData.versionName}</span>
            </div>
          </div>
          <div className="flex items-center space-x-4">
            <button 
              onClick={() => setShowOnboardingQr(true)}
              className="flex items-center space-x-2 px-4 py-2 bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 rounded-lg transition-colors border border-blue-500/20 shadow-lg">
              <QrCode size={18} />
              <span>Generate Onboarding QR</span>
            </button>
            <button onClick={handleLogout} className="flex items-center space-x-2 px-4 py-2 bg-red-500/10 hover:bg-red-500/20 text-red-400 rounded-lg transition-colors border border-red-500/20 shadow-lg">
              <LogOut size={18} />
              <span>Logout</span>
            </button>
          </div>
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
                  <th className="p-4 rounded-tl-lg">Device ID</th>
                  <th className="p-4">Customer Name</th>
                  <th className="p-4">Phone</th>
                  <th className="p-4">Last Seen</th>
                  <th className="p-4">Status</th>
                  <th className="p-4 rounded-tr-lg">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {loading ? (
                  <tr><td colSpan={6} className="p-8 text-center text-gray-500">Loading devices...</td></tr>
                ) : devices.length === 0 ? (
                  <tr><td colSpan={6} className="p-8 text-center text-gray-500">No devices registered yet. Scan the Onboarding QR to get started.</td></tr>
                ) : devices.map((dev) => (
                  <tr key={dev.deviceId} className="hover:bg-white/5 transition-colors">
                    <td className="p-4 text-xs font-mono font-bold text-blue-400">{dev.deviceId}</td>
                    <td className="p-4 font-medium">{dev.userName}</td>
                    <td className="p-4 text-gray-400 font-mono text-sm">{dev.phoneNumber}</td>
                    <td className="p-4 text-gray-400 text-sm">{new Date(dev.lastSeen).toLocaleString()}</td>
                    <td className="p-4">
                      <span className={`px-3 py-1 rounded-full text-xs font-medium border ${
                        dev.isLocked 
                          ? 'bg-red-500/10 text-red-400 border-red-500/20' 
                          : 'bg-green-500/10 text-green-400 border-green-500/20'
                      }`}>
                        {dev.isLocked ? 'Locked' : 'Active'}
                      </span>
                    </td>
                    <td className="p-4 flex space-x-2">
                      {dev.isLocked ? (
                        <button 
                          onClick={() => handleLockAction(dev.deviceId, false)}
                          className="flex items-center justify-center w-8 h-8 bg-green-600/20 hover:bg-green-600/30 text-green-400 rounded border border-green-500/20 transition-colors tooltip-trigger relative group">
                          <Unlock size={14} />
                          <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded z-10 opacity-0 group-hover:opacity-100 transition-opacity">Unlock</span>
                        </button>
                      ) : (
                        <button 
                          onClick={() => handleLockAction(dev.deviceId, true)}
                          className="flex items-center justify-center w-8 h-8 bg-red-600/20 hover:bg-red-600/30 text-red-400 rounded border border-red-500/20 transition-colors tooltip-trigger relative group">
                          <Lock size={14} />
                          <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded z-10 opacity-0 group-hover:opacity-100 transition-opacity">Lock</span>
                        </button>
                      )}
                      <button 
                        onClick={() => handleReleaseDevice(dev.deviceId)}
                        className="flex items-center justify-center w-8 h-8 bg-gray-600/20 hover:bg-gray-600/30 text-gray-400 rounded border border-gray-500/20 transition-colors tooltip-trigger relative group">
                        <Trash2 size={14} />
                        <span className="absolute -top-8 bg-black/80 px-2 py-1 text-xs rounded z-10 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">Release Device</span>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Onboarding QR Code Modal */}
      {showOnboardingQr && (
        <div className="absolute inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div className="bg-gray-900 border border-white/10 p-6 rounded-2xl shadow-2xl max-w-sm w-full animate-in fade-in zoom-in duration-200">
            <div className="flex justify-between items-center mb-6">
              <h3 className="text-lg font-bold">Onboard New Device</h3>
              <button 
                onClick={() => setShowOnboardingQr(false)}
                className="p-1 hover:bg-white/10 rounded-full transition-colors text-gray-400 hover:text-white">
                <X size={20} />
              </button>
            </div>
            
            <div className="bg-white p-4 rounded-xl flex justify-center items-center">
              <QRCodeSVG 
                value={getOnboardingUrl()} 
                size={200}
                level="H"
                includeMargin={false}
              />
            </div>
            
            <div className="mt-6 text-center">
              <p className="text-sm text-gray-400 mb-2">Scan from any new Android device</p>
              <div className="bg-black/30 p-3 rounded-lg border border-white/5 overflow-hidden text-ellipsis">
                <p className="text-xs font-mono text-blue-300 break-all">{getOnboardingUrl()}</p>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
