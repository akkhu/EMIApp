package com.scmemilocker.app

import android.app.admin.DevicePolicyManager
import android.app.admin.FactoryResetProtectionPolicy
import android.os.Bundle
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.scmemilocker.app.receivers.EmiDeviceAdminReceiver
import com.scmemilocker.app.ui.LockedActivity

/**
 * Central Device Owner policy controller for the EMI POC.
 *
 * Model:
 *  - PERSISTENT restrictions (USB, safe-boot, developer options, factory reset,
 *    uninstall/force-stop) are applied the moment the device is provisioned and
 *    stay on until the loan is RELEASED. This is what makes the lock un-bypassable:
 *    a customer cannot sideload over USB, boot into safe mode, enable ADB, factory
 *    reset, or uninstall the controller even when the phone is currently unlocked.
 *  - LOCK only adds the full-screen kiosk (LockedActivity + lock-task). UNLOCK only
 *    removes that kiosk; the persistent restrictions remain.
 *  - RELEASE removes every restriction, hands the device back, and relinquishes
 *    Device Owner so the app can be uninstalled.
 */
class DeviceRestrictionManager(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val admin = ComponentName(context, EmiDeviceAdminReceiver::class.java)
    private val prefs = context.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)

    fun isDeviceOwner(): Boolean = dpm.isDeviceOwnerApp(context.packageName)

    /**
     * Applied at provisioning and re-asserted on every boot. Blocks every common
     * route a customer could use to remove or defeat the EMI lock.
     */
    fun applyPersistentRestrictions() {
        if (!isDeviceOwner()) return

        val restrictions = listOf(
            UserManager.DISALLOW_SAFE_BOOT,          // no safe-mode reboot
            UserManager.DISALLOW_DEBUGGING_FEATURES, // no developer options / ADB
            UserManager.DISALLOW_USB_FILE_TRANSFER,  // no USB file transfer
            UserManager.DISALLOW_FACTORY_RESET,      // no factory reset
            UserManager.DISALLOW_ADD_USER,           // no extra users to escape
            UserManager.DISALLOW_APPS_CONTROL,       // no force-stop / clear-data / uninstall
            UserManager.DISALLOW_CONFIG_DATE_TIME,   // no clock tampering vs offline checks
            UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA
        )
        for (r in restrictions) {
            try {
                dpm.addUserRestriction(admin, r)
            } catch (e: Exception) {
                Log.w("EmiRestrict", "Could not set $r: ${e.message}")
            }
        }

        // Fully disable the USB data line (API 31+), the strongest USB block.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (dpm.canUsbDataSignalingBeDisabled()) {
                    dpm.isUsbDataSignalingEnabled = false
                }
            } catch (e: Exception) {
                Log.w("EmiRestrict", "USB data signaling block unsupported: ${e.message}")
            }
        }

        // Survive a recovery-mode factory reset: require the lender's Google account.
        applyFrpPolicy()

        // Whitelist ourselves for kiosk (lock-task) mode up front.
        try {
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
        } catch (e: Exception) {
            Log.w("EmiRestrict", "setLockTaskPackages failed: ${e.message}")
        }
    }

    /** Full lock: ensure restrictions are on, then show the kiosk screen. */
    fun enforceEmiLock() {
        if (!isDeviceOwner()) return
        val wasLocked = prefs.getBoolean("isLocked", false)
        prefs.edit().putBoolean("isLocked", true).apply()
        applyPersistentRestrictions()

        // Android Go cannot grant "draw over other apps", and Android only lets
        // apps open screens from the background in narrow cases. So the lock must
        // not depend on startActivity() alone:
        //  1. Our lock screen becomes the HOME screen (every Home press / reboot lands on it).
        //  2. Every other launchable app is suspended (opening one shows "app paused").
        //  3. On the first lock, the screen is turned off; on wake the user lands on our home.
        setLockHome(true)
        setOtherAppsSuspended(true)

        try {
            context.startActivity(Intent(context, LockedActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            })
        } catch (e: Exception) {
            Log.w("EmiLock", "Direct launch blocked: ${e.message}")
        }
        if (!wasLocked) {
            try { dpm.lockNow() } catch (_: Exception) {}
        }
    }

    /**
     * Unlock = remove the kiosk only. Persistent restrictions STAY because the
     * loan is not finished yet (restrictions-always-until-release model).
     */
    fun clearEmiLock() {
        if (!isDeviceOwner()) return
        prefs.edit().putBoolean("isLocked", false).apply()
        setOtherAppsSuspended(false)
        setLockHome(false)
        // LockedActivity polls and finishes itself; the normal launcher is home again.
    }

    /** Loan fully paid: strip everything and relinquish Device Owner. */
    fun releaseDevice() {
        if (!isDeviceOwner()) return
        setOtherAppsSuspended(false)
        setLockHome(false)

        val restrictions = listOf(
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_DEBUGGING_FEATURES,
            UserManager.DISALLOW_USB_FILE_TRANSFER,
            UserManager.DISALLOW_FACTORY_RESET,
            UserManager.DISALLOW_ADD_USER,
            UserManager.DISALLOW_APPS_CONTROL,
            UserManager.DISALLOW_CONFIG_DATE_TIME,
            UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA
        )
        for (r in restrictions) {
            try { dpm.clearUserRestriction(admin, r) } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try { dpm.isUsbDataSignalingEnabled = true } catch (_: Exception) {}
        }

        try { dpm.setLockTaskPackages(admin, emptyArray()) } catch (_: Exception) {}

        // Unhide the launcher icon so the customer can open & uninstall.
        try {
            val p = context.packageManager
            p.setComponentEnabledSetting(
                ComponentName(context, com.scmemilocker.app.ui.MainActivity::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (_: Exception) {}

        prefs.edit().clear().apply()

        // Open this app's settings page so Uninstall is one tap away.
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:" + context.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {}

        // Paid off: remove FRP so a reset never asks for the lender's account.
        clearFrpPolicy()

        // Relinquish Device Owner permanently (also auto-clears remaining policies).
        try { dpm.clearDeviceOwnerApp(context.packageName) } catch (_: Exception) {}
    }

    // ---------------- Factory Reset Protection ----------------

    private val GMS = "com.google.android.gms"
    private val FRP_KEY = "factoryResetProtectionAdmin"
    private val FRP_CHANGED = "com.google.android.gms.auth.FRP_CONFIG_CHANGED"

    private fun notifyGmsFrpChanged() {
        try {
            context.sendBroadcast(Intent(FRP_CHANGED).setPackage(GMS))
        } catch (_: Exception) {}
    }

    /** Ties the device to the lender's Google account(s) after any factory reset. */
    fun applyFrpPolicy() {
        if (!isDeviceOwner()) return
        val ids = Config.FRP_ACCOUNT_IDS.filter { it.isNotBlank() }
        if (ids.isEmpty()) {
            Log.w("EmiFRP", "FRP account not configured (Config.kt) - skipping")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val policy = FactoryResetProtectionPolicy.Builder()
                    .setFactoryResetProtectionAccounts(ids)
                    .setFactoryResetProtectionEnabled(true)
                    .build()
                dpm.setFactoryResetProtectionPolicy(admin, policy)
                Log.i("EmiFRP", "FRP policy set (API 30+)")
            } catch (e: Exception) {
                Log.w("EmiFRP", "FRP policy unsupported on this device: ${e.message}")
            }
        } else {
            // Android 7-10: legacy method via Google Play services app restrictions.
            try {
                val b = Bundle().apply { putStringArray(FRP_KEY, ids.toTypedArray()) }
                dpm.setApplicationRestrictions(admin, GMS, b)
                notifyGmsFrpChanged()
                Log.i("EmiFRP", "FRP set via Play services (legacy)")
            } catch (e: Exception) {
                Log.w("EmiFRP", "Legacy FRP failed: ${e.message}")
            }
        }
    }

    /** Removes the lender account requirement (called on release). */
    fun clearFrpPolicy() {
        if (!isDeviceOwner()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { dpm.setFactoryResetProtectionPolicy(admin, null) } catch (_: Exception) {}
        }
        try {
            dpm.setApplicationRestrictions(admin, GMS, Bundle())
            notifyGmsFrpChanged()
        } catch (_: Exception) {}
    }

    // ---------------- Lock enforcement that works on Android Go ----------------

    private val homeAlias = ComponentName(context.packageName, "com.scmemilocker.app.ui.LockHomeAlias")

    /** Makes the lock screen the device HOME while locked; restores the launcher on unlock. */
    private fun setLockHome(enabled: Boolean) {
        try {
            context.packageManager.setComponentEnabledSetting(
                homeAlias,
                if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) { Log.w("EmiLock", "home alias toggle failed: ${e.message}") }
        try {
            if (enabled) {
                val filter = IntentFilter(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_HOME)
                    addCategory(Intent.CATEGORY_DEFAULT)
                }
                dpm.addPersistentPreferredActivity(admin, filter, homeAlias)
            } else {
                dpm.clearPackagePersistentPreferredActivities(admin, context.packageName)
            }
        } catch (e: Exception) { Log.w("EmiLock", "preferred home failed: ${e.message}") }
    }

    /** Suspends every launchable app except ours, the phone dialer (emergency calls) and Settings. */
    private fun setOtherAppsSuspended(suspend: Boolean) {
        val key = "suspendedPkgs"
        if (suspend) {
            val keep = mutableSetOf(context.packageName, "com.android.settings")
            try {
                val tm = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
                tm.defaultDialerPackage?.let { keep.add(it) }
            } catch (_: Exception) {}
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val targets = context.packageManager.queryIntentActivities(launcherIntent, 0)
                .map { it.activityInfo.packageName }.distinct().filter { it !in keep }
            val failed = try {
                dpm.setPackagesSuspended(admin, targets.toTypedArray(), true).toSet()
            } catch (e: Exception) {
                Log.w("EmiLock", "suspend failed: ${e.message}"); targets.toSet()
            }
            val done = (prefs.getStringSet(key, emptySet()) ?: emptySet()) + targets.filter { it !in failed }
            prefs.edit().putStringSet(key, done).apply()
        } else {
            val list = prefs.getStringSet(key, emptySet()) ?: emptySet()
            if (list.isNotEmpty()) {
                try { dpm.setPackagesSuspended(admin, list.toTypedArray(), false) } catch (_: Exception) {}
            }
            prefs.edit().remove(key).apply()
        }
    }
}
