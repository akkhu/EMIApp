import React from 'react';

const PrivacyPolicy = () => {
  return (
    <div className="min-h-screen bg-gray-900 text-gray-200 p-8">
      <div className="max-w-4xl mx-auto glass p-8 space-y-6">
        <h1 className="text-3xl font-bold text-white border-b border-gray-700 pb-4">Privacy Policy for EmiLocker Device Admin</h1>
        <p className="text-sm text-gray-400">Last Updated: June 14, 2026</p>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-white">1. Introduction</h2>
          <p>
            EmiLocker ("we," "our," or "us") provides a device management and security solution for enterprise and authorized device financing administrators. This Privacy Policy explains how we collect, use, and protect information when you use the EmiLocker application ("App").
          </p>
        </section>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-blue-400">2. Device Administrator Privileges</h2>
          <p>
            <strong>This app uses Device Administrator permissions (BIND_DEVICE_ADMIN).</strong>
          </p>
          <p>
            EmiLocker is designed exclusively for enterprise and financial management of hardware. To function correctly, the App requires Device Administrator privileges. These privileges are strictly used to:
          </p>
          <ul className="list-disc pl-6 space-y-1 text-gray-300">
            <li>Restrict unauthorized factory resets that would circumvent security policies.</li>
            <li>Enforce device locking (Kiosk mode) in the event of administrative action.</li>
            <li>Prevent booting into Safe Mode to bypass security restrictions.</li>
          </ul>
          <p>
            These permissions are never used to access your personal data, intercept communications, or monitor your private usage of the device.
          </p>
        </section>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-white">3. Information We Collect</h2>
          <p>We collect only the minimum data required to uniquely identify and manage the provisioned device:</p>
          <ul className="list-disc pl-6 space-y-1 text-gray-300">
            <li><strong>Device Identifiers:</strong> We collect a unique device ID (such as Android ID) to register the device in our secure administration portal.</li>
            <li><strong>Basic Registration Info:</strong> Name and Phone Number, provided strictly for hardware administration and contact purposes.</li>
            <li><strong>Push Notification Tokens:</strong> We collect Firebase Cloud Messaging (FCM) tokens solely to send secure lock/unlock administrative commands to the device.</li>
          </ul>
        </section>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-white">4. Information We Do NOT Collect</h2>
          <p>To ensure your personal privacy, EmiLocker <strong>does not</strong> collect, track, or access:</p>
          <ul className="list-disc pl-6 space-y-1 text-gray-300">
            <li>Your physical location (GPS data).</li>
            <li>Your browsing history or internet traffic.</li>
            <li>Your personal files, photos, or videos.</li>
            <li>Your text messages, emails, or phone call logs.</li>
            <li>Your keystrokes or passwords.</li>
          </ul>
        </section>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-white">5. How We Use the Information</h2>
          <p>The collected information is used strictly to:</p>
          <ul className="list-disc pl-6 space-y-1 text-gray-300">
            <li>Register the device within the enterprise portal.</li>
            <li>Allow authorized administrators to secure or lock the device remotely.</li>
            <li>Ensure the integrity of the device management system.</li>
          </ul>
        </section>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-white">6. Data Security and Retention</h2>
          <p>
            All data transmitted between the App and our servers is encrypted using industry-standard secure protocols (HTTPS). We retain the device identifiers only as long as the device remains under active management. When a device is officially released by the administrator, the Device Owner privileges are stripped, the app uninstalls, and the registration data is securely purged from our active management systems.
          </p>
        </section>

        <section className="space-y-3">
          <h2 className="text-xl font-semibold text-white">7. User Consent and Device Release</h2>
          <p>
            By enrolling a device using the Android Enterprise provisioning method (QR Code or ADB), the administrator acknowledges and consents to these terms. Because EmiLocker is a Device Owner application, it cannot be uninstalled manually by the end-user. It can only be uninstalled when the authorized administrator clicks the "Release Device" button from the management portal, which safely and permanently removes all restrictions.
          </p>
        </section>

        <section className="space-y-3 border-t border-gray-700 pt-6 mt-6">
          <h2 className="text-xl font-semibold text-white">Contact Us</h2>
          <p>If you have any questions regarding this Privacy Policy or your device's management status, please contact your organizational IT administrator or hardware provider.</p>
        </section>
      </div>
    </div>
  );
};

export default PrivacyPolicy;
