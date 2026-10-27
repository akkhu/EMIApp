package com.scmemilocker.app.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.scmemilocker.app.DeviceRestrictionManager
import com.scmemilocker.app.R
import com.scmemilocker.app.workers.OfflineExpiryWorker
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * POC: no customer form, no login. On first launch after provisioning the app
 * silently self-registers (device id + FCM token) and then hides its icon.
 * All it ever shows is a one-line status.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var restrictions: DeviceRestrictionManager
    private val client = OkHttpClient()
    private val gson = Gson()
    private val BASE_URL = "https://emi-app-7pq1.vercel.app/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        restrictions = DeviceRestrictionManager(this)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvVersion = findViewById<TextView>(R.id.tvAppVersion)
        tvVersion.text = "v" + packageManager.getPackageInfo(packageName, 0).versionName

        if (!restrictions.isDeviceOwner()) {
            tvStatus.text = "Not provisioned as Device Owner.\nFactory reset and scan the QR."
            return
        }

        // We own the device: lock it down and register silently.
        restrictions.applyPersistentRestrictions()
        tvStatus.text = "Device secured and active."

        // Registration, polling and hiding the icon happen in RegisterWorker.
        com.scmemilocker.app.workers.RegisterWorker.enqueue(this)
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("deviceId", null) ?: return
        val request = Request.Builder().url("$BASE_URL/status?deviceId=$deviceId").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return
                val body = response.body?.string() ?: return
                val map = gson.fromJson(body, Map::class.java)
                if (map["isLocked"] as? Boolean == true) {
                    runOnUiThread { restrictions.enforceEmiLock() }
                }
            }
        })
    }
}
