package com.scmemilocker.app.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.scmemilocker.app.workers.RegisterWorker

/**
 * Android 12+ setup handshake, step 2. Mirrors Google's TestDPC: report success
 * and finish. We do NOT open our own screen or apply restrictions here (that can
 * break the setup wizard); we only queue the post-setup work.
 */
class AdminPolicyComplianceActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("EmiProvisioning", "ADMIN_POLICY_COMPLIANCE")
        RegisterWorker.enqueue(applicationContext)
        setResult(RESULT_OK)
        finish()
    }
}
