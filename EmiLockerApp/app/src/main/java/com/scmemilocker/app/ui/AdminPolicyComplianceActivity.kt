package com.scmemilocker.app.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Button
import com.scmemilocker.app.R
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
        setContentView(R.layout.activity_compliance)
        Log.i("EmiProvisioning", "AdminPolicyCompliance called - provisioning complete")

        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminName = ComponentName(this, EmiDeviceAdminReceiver::class.java)

        // Tell the Android system that we are done with the provisioning phase!
        try {
            dpm.setProfileEnabled(adminName)
        } catch (e: Exception) {
            Log.e("EmiProvisioning", "Error setting profile enabled: ${e.message}")
        }

        findViewById<Button>(R.id.btn_continue_setup).setOnClickListener {
            // Signal that policy compliance is satisfied only AFTER the user clicks continue
            setResult(RESULT_OK)
            finish()
        }
    }
}
