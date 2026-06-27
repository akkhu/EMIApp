package com.scmemilocker.app.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

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

        // Signal that policy compliance is satisfied
        setResult(RESULT_OK)
        finish()

        // Launch the main registration activity
        val launchIntent = Intent(this, MainActivity::class.java)
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(launchIntent)
    }
}
