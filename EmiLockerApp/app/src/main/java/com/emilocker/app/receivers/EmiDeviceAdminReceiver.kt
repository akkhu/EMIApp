package com.emilocker.app.receivers

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
        
        // Automatically launch MainActivity for registration
        val launchIntent = Intent(context, com.emilocker.app.ui.MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }
}
