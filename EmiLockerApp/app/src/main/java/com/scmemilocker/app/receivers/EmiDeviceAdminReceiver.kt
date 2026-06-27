package com.scmemilocker.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import android.util.Log
import com.google.gson.Gson
import com.scmemilocker.app.DeviceRestrictionManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class EmiDeviceAdminReceiver : DeviceAdminReceiver() {
    private val BASE_URL = "https://emi-app-7pq1.vercel.app/api"
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("EmiDeviceAdmin", "Device Owner Enabled")
        
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(context, EmiDeviceAdminReceiver::class.java)
        
        // Prevent users from tampering with app permissions, force stopping, or clearing data
        dpm.addUserRestriction(adminName, UserManager.DISALLOW_APPS_CONTROL)
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
            val restrictionManager = DeviceRestrictionManager(context)
            
            // Check persistence immediately
            if (prefs.getBoolean("isLocked", false)) {
                Log.i("EmiDeviceAdmin", "Device rebooted while locked, re-locking instantly.")
                restrictionManager.enforceEmiLock()
            }

            // Sync with backend on every boot just in case status changed
            val deviceId = prefs.getString("deviceId", null)
            if (deviceId != null) {
                val request = Request.Builder()
                    .url("$BASE_URL/status?deviceId=$deviceId")
                    .get()
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("EmiDeviceAdmin", "Boot sync failed", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (response.isSuccessful) {
                            val body = response.body?.string() ?: return
                            try {
                                val map = gson.fromJson(body, Map::class.java)
                                val isLocked = map["isLocked"] as? Boolean ?: false
                                if (isLocked) {
                                    restrictionManager.enforceEmiLock()
                                } else {
                                    restrictionManager.clearEmiLock()
                                }
                            } catch (e: Exception) {
                                Log.e("EmiDeviceAdmin", "JSON parse error", e)
                            }
                        }
                    }
                })
            }
        }
    }
}
