package com.emilocker.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.emilocker.app.receivers.EmiDeviceAdminReceiver
import com.emilocker.app.ui.LockedActivity

class DeviceRestrictionManager(private val context: Context) {
    
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminName = ComponentName(context, EmiDeviceAdminReceiver::class.java)

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun enforceEmiLock() {
        if (isDeviceOwner()) {
            // 1. Prevent the user from performing a Factory Reset
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_FACTORY_RESET)
            
            // 2. Prevent Safe Mode rebooting
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_SAFE_BOOT)
            
            // 3. Prevent altering system date/time to bypass offline EMI checks
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_CONFIG_DATE_TIME)

            // 4. White-list our LockedActivity for Lock Task Mode (Kiosk Mode)
            dpm.setLockTaskPackages(adminName, arrayOf(context.packageName))
            
            // 5. Fire intent to launch the Locked Screen Activity
            val lockIntent = Intent(context, LockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(lockIntent)
        }
    }
    
    fun clearEmiLock() {
        if (isDeviceOwner()) {
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_FACTORY_RESET)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_SAFE_BOOT)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_CONFIG_DATE_TIME)
            dpm.setLockTaskPackages(adminName, emptyArray())
        }
    }
}
