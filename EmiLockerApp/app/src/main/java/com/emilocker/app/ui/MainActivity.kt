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
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.google.gson.Gson
import com.emilocker.app.api.models.QrCodePayload
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class MainActivity : AppCompatActivity() {
    private lateinit var restrictionManager: DeviceRestrictionManager

    private val barcodeLauncher = registerForActivityResult(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            handleQrScanned(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        restrictionManager = DeviceRestrictionManager(this)
        val tvStatus = findViewById<TextView>(R.id.tvDeviceOwnerStatus)
        val btnLock = findViewById<Button>(R.id.btnManualLock)
        val btnScanQr = findViewById<Button>(R.id.btnScanQr)

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

        btnScanQr.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Scan EMI QR Code from Dashboard")
            options.setCameraId(0)
            options.setBeepEnabled(false)
            options.setBarcodeImageEnabled(true)
            barcodeLauncher.launch(options)
        }
        
        // Start offline background worker
        scheduleOfflineWorker()
    }

    private fun handleQrScanned(payload: String) {
        try {
            val qrData = Gson().fromJson(payload, QrCodePayload::class.java)
            if (qrData.action == "REGISTER_AND_LOCK") {
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

                sharedPrefs.edit()
                    .putString("appId", qrData.appId)
                    .putString("imei", qrData.imei)
                    .putString("serverUrl", qrData.serverUrl)
                    .apply()

                Toast.makeText(this, "Device Registered via QR! Server: ${qrData.serverUrl}", Toast.LENGTH_LONG).show()
                scheduleOfflineWorker() 
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid QR Format", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleOfflineWorker() {
        val workRequest = PeriodicWorkRequestBuilder<OfflineExpiryWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "OfflineExpiryWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}
