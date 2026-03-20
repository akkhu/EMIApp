package com.emilocker.app

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
            // 1. Prevent Factory Reset
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_FACTORY_RESET)
            
            // 2. Prevent Safe Mode rebooting
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_SAFE_BOOT)
            
            // 3. Prevent changing date/time
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_CONFIG_DATE_TIME)

            // 4. Block USB file transfer and debugging
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER)
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES)

            // 5. Block uninstalling apps (prevents removing this app)
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_INSTALL_APPS)
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_UNINSTALL_APPS)

            // 6. Block access to Settings entirely
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_CONFIG_CREDENTIALS)

            // 7. Prevent adding/removing accounts (blocks Google account tricks)
            dpm.addUserRestriction(adminName, UserManager.DISALLOW_MODIFY_ACCOUNTS)

            // 8. Hide the app from the launcher so user can't find it
            dpm.setApplicationHidden(adminName, context.packageName, false) // Keep OUR app visible for lock screen
            
            // 9. White-list our LockedActivity for Lock Task Mode (Kiosk Mode)
            dpm.setLockTaskPackages(adminName, arrayOf(context.packageName))
            
            // 10. Fire intent to launch the Locked Screen Activity
            val lockIntent = Intent(context, LockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            context.startActivity(lockIntent)
        }
    }
    
    fun clearEmiLock() {
        if (isDeviceOwner()) {
            // Remove all restrictions
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_FACTORY_RESET)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_SAFE_BOOT)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_CONFIG_DATE_TIME)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_INSTALL_APPS)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_UNINSTALL_APPS)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_CONFIG_CREDENTIALS)
            dpm.clearUserRestriction(adminName, UserManager.DISALLOW_MODIFY_ACCOUNTS)
            dpm.setLockTaskPackages(adminName, emptyArray())
        }
    }

    fun hideAppFromLauncher() {
        // Disable the launcher activity so the app disappears from the home screen
        val launcherComponent = ComponentName(context, "com.emilocker.app.ui.MainActivity")
        context.packageManager.setComponentEnabledSetting(
            launcherComponent,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    fun showAppInLauncher() {
        // Re-enable the launcher activity so the app reappears
        val launcherComponent = ComponentName(context, "com.emilocker.app.ui.MainActivity")
        context.packageManager.setComponentEnabledSetting(
            launcherComponent,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
