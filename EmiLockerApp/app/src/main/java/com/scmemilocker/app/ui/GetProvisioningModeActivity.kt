package com.scmemilocker.app.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * Android 12+ setup handshake, step 1. Mirrors Google's TestDPC: reply with
 * ONLY the provisioning mode (fully managed) and finish immediately. No extra
 * result extras, no delays, no UI.
 */
class GetProvisioningModeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fullyManaged = DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        val allowed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            intent.getIntegerArrayListExtra(DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES)
        else null
        Log.i("EmiProvisioning", "GET_PROVISIONING_MODE allowed=$allowed")
        com.scmemilocker.app.ProvLog.send(this, "get_provisioning_mode", "allowed=$allowed")

        if (allowed != null && allowed.isNotEmpty() && !allowed.contains(fullyManaged)) {
            setResult(RESULT_CANCELED)
        } else {
            setResult(RESULT_OK, Intent().putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, fullyManaged))
        }
        finish()
    }
}
