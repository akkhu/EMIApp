package com.scmemilocker.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class EmiDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("EmiDeviceAdmin", "Device Owner Enabled")
        // Trigger initial policies
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i("EmiDeviceAdmin", "Provisioning Complete")
        
        // Force launch the main app to break out of Setup Wizard (Fallback for older Android)
        val launchIntent = Intent(context, com.scmemilocker.app.ui.MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(launchIntent)
    }
}
