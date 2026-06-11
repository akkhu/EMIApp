package com.emilocker.app.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.R
import com.emilocker.app.workers.OfflineExpiryWorker
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var restrictionManager: DeviceRestrictionManager
    private val client = OkHttpClient()
    private val gson = Gson()
    
    // Replace with your Vercel URL after deployment
    private val BASE_URL = "https://emi-app-7pq1.vercel.app/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        restrictionManager = DeviceRestrictionManager(this)
        
        val tvStatus = findViewById<TextView>(R.id.tvDeviceOwnerStatus)
        val etName = findViewById<EditText>(R.id.etName)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        if (!restrictionManager.isDeviceOwner()) {
            tvStatus.text = "Status: Device Admin NOT Provisioned"
            tvStatus.setTextColor(android.graphics.Color.RED)
            Toast.makeText(this, "Please provision as Device Owner via ADB first.", Toast.LENGTH_LONG).show()
        } else {
            tvStatus.text = "Status: Device Ready for Registration"
            tvStatus.setTextColor(android.graphics.Color.BLUE)
        }

        val prefs = getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        val isRegistered = prefs.getBoolean("isRegistered", false)
        if (isRegistered) {
            etName.isEnabled = false
            etPhone.isEnabled = false
            btnRegister.isEnabled = false
            btnRegister.text = "Already Registered"
            tvStatus.text = "Status: Active & Polling"
            tvStatus.setTextColor(android.graphics.Color.GREEN)
            scheduleWorker()
        }

        btnRegister.setOnClickListener {
            val name = etName.text.toString()
            val phone = etPhone.text.toString()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var deviceId = prefs.getString("deviceId", null)
            if (deviceId == null) {
                deviceId = UUID.randomUUID().toString()
                prefs.edit().putString("deviceId", deviceId).apply()
            }

            btnRegister.isEnabled = false
            btnRegister.text = "Fetching FCM..."

            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Failed to get FCM token", Toast.LENGTH_SHORT).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register Device"
                    return@addOnCompleteListener
                }
                val fcmToken = task.result
                registerDevice(deviceId, name, phone, fcmToken, tvStatus, btnRegister, etName, etPhone)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("isRegistered", false)) {
            val deviceId = prefs.getString("deviceId", null) ?: return
            
            // Self-healing: if FCM token wasn't saved locally, fetch it and update backend
            if (prefs.getString("fcmToken", null) == null) {
                com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val fcmToken = task.result
                        prefs.edit().putString("fcmToken", fcmToken).apply()
                        
                        // Resend to backend to ensure it has the token
                        val map = mapOf(
                            "deviceId" to deviceId, 
                            "userName" to "akhilesh", // default fallback if needed
                            "phoneNumber" to "000000000",
                            "fcmToken" to fcmToken
                        )
                        val body = gson.toJson(map).toRequestBody("application/json; charset=utf-8".toMediaType())
                        client.newCall(Request.Builder().url("$BASE_URL/register").post(body).build()).enqueue(object: Callback {
                            override fun onFailure(call: Call, e: IOException) {}
                            override fun onResponse(call: Call, response: Response) {}
                        })
                    }
                }
            }

            val request = Request.Builder()
                .url("$BASE_URL/status?deviceId=$deviceId")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val body = response.body?.string() ?: return
                        val map = gson.fromJson(body, Map::class.java)
                        val isLocked = map["isLocked"] as? Boolean ?: false
                        if (isLocked) {
                            runOnUiThread {
                                restrictionManager.enforceEmiLock()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun registerDevice(deviceId: String, name: String, phone: String, fcmToken: String, tvStatus: TextView, btnRegister: Button, etName: EditText, etPhone: EditText) {
        val map = mapOf("deviceId" to deviceId, "userName" to name, "phoneNumber" to phone, "fcmToken" to fcmToken)
        val json = gson.toJson(map)
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/register")
            .post(body)
            .build()

        runOnUiThread {
            btnRegister.isEnabled = false
            btnRegister.text = "Registering..."
        }

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Network Error: ${e.message}", Toast.LENGTH_LONG).show()
                    btnRegister.isEnabled = true
                    btnRegister.text = "Register Device"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
                            .edit()
                            .putBoolean("isRegistered", true)
                            .putString("fcmToken", fcmToken)
                            .apply()
                            
                        Toast.makeText(this@MainActivity, "Registration Successful! App will now hide.", Toast.LENGTH_LONG).show()
                        tvStatus.text = "Status: Active & Polling"
                        tvStatus.setTextColor(android.graphics.Color.GREEN)
                        btnRegister.text = "Already Registered"
                        etName.isEnabled = false
                        etPhone.isEnabled = false
                        
                        scheduleWorker()

                        // Hide the app icon from the launcher
                        val p = packageManager
                        val componentName = android.content.ComponentName(this@MainActivity, MainActivity::class.java)
                        p.setComponentEnabledSetting(componentName, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP)

                    } else {
                        Toast.makeText(this@MainActivity, "Server Error: ${response.code}", Toast.LENGTH_LONG).show()
                        btnRegister.isEnabled = true
                        btnRegister.text = "Register Device"
                    }
                }
            }
        })
    }

    private fun scheduleWorker() {
        // Run polling every 15 minutes (WorkManager minimum)
        val workRequest = PeriodicWorkRequestBuilder<OfflineExpiryWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "LockStatusWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
