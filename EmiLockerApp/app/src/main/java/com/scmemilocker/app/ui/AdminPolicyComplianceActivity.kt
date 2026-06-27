package com.scmemilocker.app.ui

import android.app.Activity
import android.os.Bundle
import android.util.Log

class AdminPolicyComplianceActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("EmiProvisioning", "AdminPolicyCompliance called")

        setResult(RESULT_OK)
        
        // Force launch the main app to break out of Setup Wizard
        val launchIntent = android.content.Intent(this, MainActivity::class.java).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(launchIntent)

        finish()
    }
}
