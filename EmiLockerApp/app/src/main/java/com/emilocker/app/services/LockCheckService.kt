package com.emilocker.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.R
import com.emilocker.app.api.ApiClient
import kotlinx.coroutines.*

class LockCheckService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val CHANNEL_ID = "emi_lock_channel"
    private val NOTIFICATION_ID = 1001

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        startPolling()
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                checkStatus()
                delay(60_000) // Check every 60 seconds
            }
        }
    }

    private suspend fun checkStatus() {
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

            val appId = sharedPrefs.getString("appId", null) ?: return
            val imei = sharedPrefs.getString("imei", null) ?: return
            val serverUrl = sharedPrefs.getString("serverUrl", null) ?: return

            val response = ApiClient.getService(serverUrl).getDeviceStatus(appId, imei)
            val manager = DeviceRestrictionManager(applicationContext)

            if (response.isSuccessful) {
                val status = response.body()?.status
                Log.d("LockCheckService", "Server status: $status")
                
                if (status == "Locked") {
                    manager.enforceEmiLock()
                } else if (status == "Unlocked") {
                    manager.clearEmiLock()
                }
                sharedPrefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
            }
        } catch (e: Exception) {
            Log.e("LockCheckService", "Polling error", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EMI Security Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors EMI payment status"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EMI Security Active")
            .setContentText("Device protection is running")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
