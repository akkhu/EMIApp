package com.emilocker.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.R
import com.emilocker.app.services.LockCheckService
import com.emilocker.app.workers.OfflineExpiryWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var restrictionManager: DeviceRestrictionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if device is already registered with Aadhaar
        if (!isDeviceRegistered()) {
            // Not registered yet! Redirect to the Registration Form
            startActivity(Intent(this, RegistrationActivity::class.java))
            finish()
            return
        }

        // If registered, show a simple status screen (though typically the app is hidden at this point)
        setContentView(R.layout.activity_main)
        restrictionManager = DeviceRestrictionManager(this)
        
        val tvStatus = findViewById<TextView>(R.id.tvDeviceOwnerStatus)
        if (restrictionManager.isDeviceOwner()) {
            tvStatus.text = "Device is Registered and Secured.\n(This app icon should have been hidden)"
            tvStatus.setTextColor(android.graphics.Color.GREEN)
        } else {
            tvStatus.text = "Error: Not Device Owner!"
            tvStatus.setTextColor(android.graphics.Color.RED)
        }

        // Ensure background services are running
        val serviceIntent = Intent(this, LockCheckService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        scheduleOfflineWorker()
    }

    private fun isDeviceRegistered(): Boolean {
        try {
            val masterKey = MasterKey.Builder(applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPrefs = EncryptedSharedPreferences.create(
                applicationContext,
                "emi_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // If we have an aadhaarNumber saved, it means the registration form was successfully submitted
            return sharedPrefs.getString("aadhaarNumber", null) != null
        } catch (e: Exception) {
            return false
        }
    }

    private fun scheduleOfflineWorker() {
        val workRequest = PeriodicWorkRequestBuilder<OfflineExpiryWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineExpiryWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
