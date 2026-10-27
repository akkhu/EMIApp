package com.scmemilocker.app.ui

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scmemilocker.app.DeviceRestrictionManager
import com.scmemilocker.app.R
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

/**
 * Full-screen kiosk shown while the device is LOCKED. Lock-task pinning prevents
 * leaving via Home/Recents/Back. It polls the backend every 15s so a dashboard
 * Unlock or Release dismisses it automatically, and also reacts to FCM instantly.
 */
class LockedActivity : AppCompatActivity() {

    private val BASE_URL = "https://emi-app-7pq1.vercel.app/api"
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var poll: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locked)

        try { startLockTask() } catch (_: Exception) {}

        findViewById<Button>(R.id.btnCheckStatus).setOnClickListener { checkStatus(true) }

        // Hidden maintenance escape for POC testing: tap the title 10x quickly.
        var clicks = 0
        var last = 0L
        val title = findViewById<TextView>(R.id.tvLockTitle)
        val emergency = findViewById<Button>(R.id.btnEmergencyRelease)
        title.setOnClickListener {
            val now = System.currentTimeMillis()
            if (now - last > 1500) clicks = 0
            last = now
            if (++clicks >= 10) {
                clicks = 0
                emergency.visibility = View.VISIBLE
                Toast.makeText(this, "Maintenance release enabled", Toast.LENGTH_SHORT).show()
            }
        }
        emergency.setOnClickListener {
            try { stopLockTask() } catch (_: Exception) {}
            DeviceRestrictionManager(this).releaseDevice()
            finish()
        }

        poll = object : Runnable {
            override fun run() {
                checkStatus(false)
                handler.postDelayed(this, 15_000)
            }
        }
    }

    override fun onResume() { super.onResume(); handler.postDelayed(poll, 15_000) }
    override fun onPause() { super.onPause(); handler.removeCallbacks(poll) }

    private fun checkStatus(interactive: Boolean) {
        val prefs = getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        val deviceId = prefs.getString("deviceId", null) ?: return
        val request = Request.Builder().url("$BASE_URL/status?deviceId=$deviceId").get().build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (interactive) runOnUiThread {
                    Toast.makeText(this@LockedActivity, "No connection", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onResponse(call: Call, response: Response) {
                when {
                    response.isSuccessful -> {
                        val json = JSONObject(response.body?.string() ?: "{}")
                        if (!json.optBoolean("isLocked", true)) dismiss(false)
                        else if (interactive) runOnUiThread {
                            Toast.makeText(this@LockedActivity, "Still locked", Toast.LENGTH_SHORT).show()
                        }
                    }
                    response.code == 404 -> dismiss(true) // deleted = released
                }
            }
        })
    }

    private fun dismiss(released: Boolean) {
        runOnUiThread {
            try { stopLockTask() } catch (_: Exception) {}
            val mgr = DeviceRestrictionManager(this)
            if (released) mgr.releaseDevice() else mgr.clearEmiLock()
            finish()
        }
    }

    @Deprecated("Blocked on purpose")
    override fun onBackPressed() { /* trap */ }
}
