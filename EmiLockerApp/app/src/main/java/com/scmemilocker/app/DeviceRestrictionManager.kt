package com.scmemilocker.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserManager
import com.scmemilocker.app.receivers.EmiDeviceAdminReceiver
import com.scmemilocker.app.ui.LockedActivity

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

    fun releaseDevice() {
        if (isDeviceOwner()) {
            clearEmiLock()
            
            // Unhide the MainActivity so the user can easily uninstall it
            val p = context.packageManager
            val componentName = ComponentName(context, com.scmemilocker.app.ui.MainActivity::class.java)
            p.setComponentEnabledSetting(componentName, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP)
            
            // Clear SharedPreferences
            context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE).edit().clear().apply()

            // Open the App Settings page so they can click Uninstall immediately!
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:" + context.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            // Remove Device Owner permanently
            dpm.clearDeviceOwnerApp(context.packageName)
        }
    }
}
