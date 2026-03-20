package com.emilocker.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.emilocker.app.DeviceRestrictionManager
import com.emilocker.app.R
import com.emilocker.app.workers.OfflineExpiryWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var restrictionManager: DeviceRestrictionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        restrictionManager = DeviceRestrictionManager(this)
        val tvStatus = findViewById<TextView>(R.id.tvDeviceOwnerStatus)
        val btnLock = findViewById<Button>(R.id.btnManualLock)

        if (restrictionManager.isDeviceOwner()) {
            tvStatus.text = "Status: Device Owner Configured. Secure."
            tvStatus.setTextColor(android.graphics.Color.GREEN)
        } else {
            tvStatus.text = "Status: Not Provisioned (Run ADB Command)"
            tvStatus.setTextColor(android.graphics.Color.RED)
        }

        btnLock.setOnClickListener {
            if (restrictionManager.isDeviceOwner()) {
                restrictionManager.enforceEmiLock()
            } else {
                Toast.makeText(this, "Not device owner yet! Provision via ADB first.", Toast.LENGTH_LONG).show()
            }
        }
        
        // Start offline background worker
        scheduleOfflineWorker()
    }

    private fun scheduleOfflineWorker() {
        val workRequest = PeriodicWorkRequestBuilder<OfflineExpiryWorker>(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineExpiryWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
