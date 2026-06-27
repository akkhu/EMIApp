package com.scmemilocker.app.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.scmemilocker.app.R
import com.scmemilocker.app.receivers.EmiDeviceAdminReceiver

class AdminPolicyComplianceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compliance)
        Log.i("EmiProvisioning", "AdminPolicyCompliance called - provisioning complete")

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, EmiDeviceAdminReceiver::class.java)

        // Try to enable the profile (safe to fail if already enabled or not applicable)
        try {
            dpm.setProfileEnabled(adminName)
        } catch (e: Exception) {
            Log.e("EmiProvisioning", "Error setting profile enabled: ${e.message}")
        }

        findViewById<Button>(R.id.btn_continue_setup).setOnClickListener {
            // Launch main app
            val launchIntent = Intent(this, MainActivity::class.java)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(launchIntent)
            
            // Signal that policy compliance is satisfied
            setResult(RESULT_OK)
            finish()
        }
    }
}
