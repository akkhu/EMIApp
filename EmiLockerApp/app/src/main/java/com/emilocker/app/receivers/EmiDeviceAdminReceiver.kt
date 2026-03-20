package com.emilocker.app.receivers

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.services.LockCheckService

class EmiDeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.i("EmiDeviceAdmin", "Device Owner Enabled")
    }

    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.i("EmiDeviceAdmin", "Provisioning Complete! Reading extras...")

        // Read custom extras passed through the provisioning QR code
        val extras = intent.getParcelableExtra<android.os.PersistableBundle>(
            android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE
        )

        val appId = extras?.getString("appId") ?: "EMI_PRO"

        Log.i("EmiDeviceAdmin", "QR Data: appId=$appId")

        if (appId.isNotEmpty()) {
            // Save registration data securely
            try {
                val masterKey = MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                val sharedPrefs = EncryptedSharedPreferences.create(
                    context,
                    "emi_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )

                sharedPrefs.edit()
                    .putString("appId", appId)
                    .apply()

                Log.i("EmiDeviceAdmin", "Device auto-provisioned! Launching Registration UI...")

                // Start the Registration Activity to collect Customer info
                val regIntent = Intent(context, com.emilocker.app.ui.RegistrationActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                context.startActivity(regIntent)

                // Start the background polling service
                val serviceIntent = Intent(context, LockCheckService::class.java)
                context.startForegroundService(serviceIntent)

            } catch (e: Exception) {
                Log.e("EmiDeviceAdmin", "Failed to save QR extras", e)
            }
        }
    }
}
