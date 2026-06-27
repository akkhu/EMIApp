package com.scmemilocker.app.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.scmemilocker.app.receivers.EmiDeviceAdminReceiver

/**
 * Required by Android 12+ for Device Owner provisioning.
 * The system calls this activity after provisioning completes to let the
 * DPC confirm that it has applied all necessary policies.
 *
 * Without this activity, provisioning hangs on "Just a sec" because the
 * system is waiting for the DPC to signal that setup is complete.
 */
class AdminPolicyComplianceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("EmiProvisioning", "AdminPolicyCompliance called - provisioning complete")

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, EmiDeviceAdminReceiver::class.java)

        // Tell the Android system that we are done with the provisioning phase!
        try {
            dpm.setProfileEnabled(adminName)
        } catch (e: Exception) {
            Log.e("EmiProvisioning", "Error setting profile enabled: ${e.message}")
        }

        // Signal that policy compliance is satisfied
        setResult(RESULT_OK)
        finish()

        // Launch the main registration activity
        val launchIntent = Intent(this, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(launchIntent)
    }
}
