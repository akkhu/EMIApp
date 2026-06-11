package com.emilocker.app.ui

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class LockedActivity : AppCompatActivity() {

    // Replace with your Vercel URL after deployment
    private val BASE_URL = "http://10.0.2.2:5173/api"
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locked)

        // Lock the task, meaning the user cannot use recent apps or home buttons to leave.
        startLockTask()

        val btnCheckStatus = findViewById<Button>(R.id.btnCheckStatus)
        btnCheckStatus.setOnClickListener {
            checkStatus(btnCheckStatus)
        }
    }

    private fun checkStatus(btn: Button) {
        val prefs = getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("deviceId", null)
        
        if (deviceId == null) {
            Toast.makeText(this, "Device ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        btn.isEnabled = false
        btn.text = "Checking..."

        val request = Request.Builder()
            .url("$BASE_URL/status?deviceId=$deviceId")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LockedActivity, "Network error", Toast.LENGTH_SHORT).show()
                    btn.isEnabled = true
                    btn.text = "Check Payment Status"
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        val json = JSONObject(bodyStr)
                        val isLocked = json.optBoolean("isLocked", true)

                        if (!isLocked) {
                            Toast.makeText(this@LockedActivity, "Unlocked! Device Restored.", Toast.LENGTH_LONG).show()
                            stopLockTask()
                            DeviceRestrictionManager(this@LockedActivity).clearEmiLock()
                            finish()
                        } else {
                            Toast.makeText(this@LockedActivity, "Status is still locked.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@LockedActivity, "Server Error", Toast.LENGTH_SHORT).show()
                    }
                    btn.isEnabled = true
                    btn.text = "Check Payment Status"
                }
            }
        })
    }

    // Modern replacement for onBackPressed() handling is via OnBackPressedDispatcher
    // But overriding the method to do nothing is a simple lock mechanism
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing to prevent exiting the lock screen
    }
}
