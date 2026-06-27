package com.scmemilocker.app.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log

class AdminPolicyComplianceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("EmiProvisioning", "AdminPolicyCompliance called")

        setResult(RESULT_OK)
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            finish()
        }, 500)
    }
}
