package com.scmemilocker.app.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * Required by Android 12+ for Device Owner provisioning.
 * The system calls this activity after downloading and installing the DPC
 * to determine the provisioning mode (Device Owner vs. Profile Owner).
 *
 * Without this activity, provisioning hangs on "Just a sec" because the
 * system has no way to hand off control to the DPC.
 */
class GetProvisioningModeActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("EmiProvisioning", "GetProvisioningMode called")

        val fullyManagedMode = DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        val allowedModes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent.getIntegerArrayListExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_ALLOWED_PROVISIONING_MODES
            )
        } else {
            null
        }

        if (allowedModes != null && !allowedModes.contains(fullyManagedMode)) {
            Log.e(
                "EmiProvisioning",
                "Fully managed provisioning is not allowed. Allowed modes: $allowedModes"
            )
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val resultIntent = Intent().apply {
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_MODE, fullyManagedMode)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_EDUCATION_SCREENS, true)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_LEAVE_ALL_SYSTEM_APPS_ENABLED, true)
            putExtra(DevicePolicyManager.EXTRA_PROVISIONING_SKIP_ENCRYPTION, true)
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
