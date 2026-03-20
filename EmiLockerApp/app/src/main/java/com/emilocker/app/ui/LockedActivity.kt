package com.emilocker.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.R
import com.emilocker.app.api.ApiClient
import kotlinx.coroutines.*

class LockedActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var tvMessage: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locked)

        // Lock the task, meaning the user cannot use recent apps or home buttons to leave.
        startLockTask()

        tvMessage = findViewById(R.id.tvLockedMessage)

        findViewById<Button>(R.id.btnCheckStatus).setOnClickListener {
            checkServerForUnlock()
        }

        // Emergency unlock code
        val etCode = findViewById<EditText>(R.id.etUnlockCode)
        findViewById<Button>(R.id.btnEmergencyUnlock).setOnClickListener {
            val code = etCode.text.toString().trim()
            if (code == "EMI") {
                tvMessage.text = "Emergency unlock activated!"
                val manager = DeviceRestrictionManager(applicationContext)
                manager.clearEmiLock()
                stopLockTask()
                finish()
            } else {
                tvMessage.text = "Invalid unlock code."
            }
        }

        // Also auto-check every 30 seconds
        startAutoCheck()
    }

    private fun checkServerForUnlock() {
        tvMessage.text = "Checking payment status with server..."

        scope.launch {
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

                val appId = sharedPrefs.getString("appId", null)
                val imei = sharedPrefs.getString("imei", null)
                val serverUrl = sharedPrefs.getString("serverUrl", null)

                if (appId == null || imei == null || serverUrl == null) {
                    tvMessage.text = "Device not registered. Cannot check status."
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    ApiClient.getService(serverUrl).getDeviceStatus(appId, imei)
                }

                if (response.isSuccessful) {
                    val status = response.body()?.status
                    if (status == "Unlocked") {
                        tvMessage.text = "Payment received! Unlocking device..."
                        val manager = DeviceRestrictionManager(applicationContext)
                        manager.clearEmiLock()
                        
                        // Stop Lock Task Mode so user can leave
                        stopLockTask()
                        
                        // Close this activity
                        finish()
                    } else {
                        tvMessage.text = "Device is still locked. Please make your EMI payment."
                    }
                } else {
                    tvMessage.text = "Server error (${response.code()}). Tap to retry."
                }
            } catch (e: Exception) {
                tvMessage.text = "Network error. Please check internet connection."
            }
        }
    }

    private fun startAutoCheck() {
        scope.launch {
            while (isActive) {
                delay(30_000) // Check every 30 seconds
                checkServerForUnlock()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing to prevent exiting the lock screen
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
