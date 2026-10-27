package com.scmemilocker.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import com.scmemilocker.app.DeviceRestrictionManager
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

class EmiDeviceAdminReceiver : DeviceAdminReceiver() {

    private val BASE_URL = "https://emi-app-7pq1.vercel.app/api"
    private val client = OkHttpClient()
    private val gson = Gson()

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("EmiDeviceAdmin", "Device Owner enabled")
        com.scmemilocker.app.ProvLog.send(context, "device_admin_enabled")
        // Don't touch policies mid-setup; RegisterWorker applies them right after.
        com.scmemilocker.app.workers.RegisterWorker.enqueue(context)
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i("EmiDeviceAdmin", "Provisioning complete")
        com.scmemilocker.app.ProvLog.send(context, "provisioning_complete")
        com.scmemilocker.app.workers.RegisterWorker.enqueue(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
            val restrictionManager = DeviceRestrictionManager(context)

            // Re-assert the hard restrictions on every boot, but only after setup.
            if (com.scmemilocker.app.workers.RegisterWorker.setupFinished(context)) {
                restrictionManager.applyPersistentRestrictions()
            }
            com.scmemilocker.app.workers.RegisterWorker.enqueue(context)

            // Re-lock immediately if we were locked before reboot.
            if (prefs.getBoolean("isLocked", false)) {
                restrictionManager.enforceEmiLock()
            }

            // Sync with backend in case status changed while powered off.
            val deviceId = prefs.getString("deviceId", null) ?: return
            val request = okhttp3.Request.Builder()
                .url("$BASE_URL/status?deviceId=$deviceId")
                .get()
                .build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("EmiDeviceAdmin", "Boot sync failed", e)
                }
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) return
                    val body = response.body?.string() ?: return
                    try {
                        val map = gson.fromJson(body, Map::class.java)
                        val isLocked = map["isLocked"] as? Boolean ?: false
                        if (map["released"] as? Boolean == true) restrictionManager.releaseDevice()
                        else if (isLocked) restrictionManager.enforceEmiLock()
                        else restrictionManager.clearEmiLock()
                    } catch (e: Exception) {
                        Log.e("EmiDeviceAdmin", "JSON parse error", e)
                    }
                }
            })
        }
    }
}
