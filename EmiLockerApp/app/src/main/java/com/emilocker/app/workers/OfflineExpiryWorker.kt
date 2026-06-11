package com.emilocker.app.workers

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.emilocker.app.DeviceRestrictionManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OfflineExpiryWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    // Replace with your Vercel URL after deployment
    private val BASE_URL = "http://10.0.2.2:5173/api"
    private val client = OkHttpClient()

    override fun doWork(): Result {
        Log.d("OfflineExpiryWorker", "Checking EMI status from backend...")
        
        try {
            val prefs = applicationContext.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
            val deviceId = prefs.getString("deviceId", null)
            
            if (deviceId == null) {
                Log.w("OfflineExpiryWorker", "Device not registered yet.")
                return Result.success()
            }

            val request = Request.Builder()
                .url("$BASE_URL/status?deviceId=$deviceId")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val isLocked = json.optBoolean("isLocked", false)

                val manager = DeviceRestrictionManager(applicationContext)
                
                if (isLocked) {
                    Log.w("OfflineExpiryWorker", "Backend reported LOCKED status. Locking device.")
                    manager.enforceEmiLock()
                } else {
                    Log.i("OfflineExpiryWorker", "Backend reported ACTIVE status. Clearing lock.")
                    manager.clearEmiLock()
                }
                
                return Result.success()
            } else {
                Log.e("OfflineExpiryWorker", "Server returned error: ${response.code}")
                return Result.retry()
            }
        } catch (e: Exception) {
            Log.e("OfflineExpiryWorker", "Network or parsing error", e)
            return Result.retry()
        }
    }
}
