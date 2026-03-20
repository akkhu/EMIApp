package com.emilocker.app.workers

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OfflineExpiryWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("OfflineExpiryWorker", "Checking EMI status online...")

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

            val appId = sharedPrefs.getString("appId", "EMI_PRO")
            val aadhaarNumber = sharedPrefs.getString("aadhaarNumber", null)

            val manager = DeviceRestrictionManager(applicationContext)

            if (appId == null || aadhaarNumber == null) {
                Log.w("OfflineExpiryWorker", "Device not registered via QR yet!")
                return@withContext Result.success()
            }

            // Make the API call
            val apiService = ApiClient.getService()
            val response = apiService.getDeviceStatus(appId, aadhaarNumber)

            if (response.isSuccessful) {
                val status = response.body()?.status
                if (status == "Locked") {
                    Log.w("OfflineExpiryWorker", "Server says locked. Enforcing lock!")
                    manager.enforceEmiLock()
                } else {
                    Log.i("OfflineExpiryWorker", "Server says unlocked. Clearing lock.")
                    manager.clearEmiLock()
                }
                
                // Save last successful sync time
                sharedPrefs.edit().putLong("last_sync_time", System.currentTimeMillis()).apply()
            } else {
                Log.e("OfflineExpiryWorker", "API call failed with code: ${response.code()}")
                enforceOfflineFallback(sharedPrefs, manager)
            }

        } catch (e: Exception) {
            Log.e("OfflineExpiryWorker", "Network or pref exception", e)
            return@withContext Result.retry()
        }

        return@withContext Result.success()
    }

    private fun enforceOfflineFallback(sharedPrefs: android.content.SharedPreferences, manager: DeviceRestrictionManager) {
        // Fallback: Lock if hasn't synced for 7 days
        val lastSync = sharedPrefs.getLong("last_sync_time", System.currentTimeMillis())
        val sevenDaysMs = 7L * 24L * 60L * 60L * 1000L
        if (System.currentTimeMillis() - lastSync > sevenDaysMs) {
            Log.w("OfflineExpiryWorker", "Offline too long! Enforcing lock.")
            manager.enforceEmiLock()
        }
    }
}
