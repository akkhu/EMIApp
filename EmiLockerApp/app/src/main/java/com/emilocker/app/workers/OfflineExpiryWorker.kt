package com.emilocker.app.workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.emilocker.app.DeviceRestrictionManager
import java.util.Date

class OfflineExpiryWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.d("OfflineExpiryWorker", "Checking EMI expiry offline...")
        
        try {
            val masterKey = MasterKey.Builder(applicationContext, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
                applicationContext,
                "emi_secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            // For testing: Suppose the expiry is set to 1 hour ago
            val defaultExpiry = System.currentTimeMillis() - 3600000 
            val expiryTimestamp = sharedPrefs.getLong("emi_expiry_timestamp", defaultExpiry)

            if (System.currentTimeMillis() > expiryTimestamp) {
                // Time has passed expiry, trigger lock
                Log.w("OfflineExpiryWorker", "EMI Expired! Locking device.")
                val manager = DeviceRestrictionManager(applicationContext)
                manager.enforceEmiLock()
            } else {
                Log.i("OfflineExpiryWorker", "EMI is within schedule. Expiry: ${Date(expiryTimestamp)}")
            }
        } catch (e: Exception) {
            Log.e("OfflineExpiryWorker", "Error securely reading prefs", e)
            return Result.retry()
        }

        return Result.success()
    }
}
