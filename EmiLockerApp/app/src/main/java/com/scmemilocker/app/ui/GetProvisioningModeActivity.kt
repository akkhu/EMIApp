package com.scmemilocker.app.ui

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
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
class GetProvisioningModeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i("EmiProvisioning", "GetProvisioningMode called")

        // We always want fully managed device (Device Owner) mode
        val resultIntent = Intent()
        resultIntent.putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE
        )

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
