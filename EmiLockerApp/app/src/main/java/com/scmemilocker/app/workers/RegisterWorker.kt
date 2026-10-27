package com.scmemilocker.app.workers

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import com.scmemilocker.app.DeviceRestrictionManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Runs AFTER setup finishes (queued from the setup handshake / boot / app open):
 * applies the hard restrictions, self-registers with the backend, starts polling
 * and hides the app icon. Keeping this out of the setup wizard avoids
 * "Something went wrong" on strict OEM / Android Go setup flows.
 */
class RegisterWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {

    private val BASE_URL = "https://emi-app-7pq1.vercel.app/api"

    override fun doWork(): Result {
        val ctx = applicationContext
        val mgr = DeviceRestrictionManager(ctx)
        if (!mgr.isDeviceOwner()) return Result.success()

        // Never touch policies while Android's setup is still running: the setup
        // wizard needs to change apps/settings after "Next", and our restrictions
        // made it abort with "Something went wrong". Google's TestDPC also waits.
        if (!setupFinished(ctx)) {
            com.scmemilocker.app.ProvLog.send(ctx, "waiting_setup_complete", "attempt=$runAttemptCount")
            return Result.retry()
        }

        com.scmemilocker.app.ProvLog.send(ctx, "post_setup_worker", "setupComplete=true")
        mgr.applyPersistentRestrictions()

        val prefs = ctx.getSharedPreferences("emi_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("isRegistered", false)) {
            schedulePolling(ctx)
            return Result.success()
        }

        val deviceId = prefs.getString("deviceId", null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString("deviceId", it).apply() }

        val token = try {
            Tasks.await(FirebaseMessaging.getInstance().token, 30, TimeUnit.SECONDS)
        } catch (e: Exception) {
            Log.w("EmiRegister", "FCM token unavailable: ${e.message}"); ""
        }

        val body = Gson().toJson(
            mapOf(
                "deviceId" to deviceId,
                "userName" to (Build.MANUFACTURER + " " + Build.MODEL),
                "phoneNumber" to "",
                "fcmToken" to token
            )
        ).toRequestBody("application/json; charset=utf-8".toMediaType())

        return try {
            val ok = OkHttpClient().newCall(
                Request.Builder().url("$BASE_URL/register").post(body).build()
            ).execute().use { it.isSuccessful }
            if (!ok) { com.scmemilocker.app.ProvLog.send(ctx, "register_http_fail"); return Result.retry() }
            com.scmemilocker.app.ProvLog.send(ctx, "registered", "deviceId=$deviceId")

            prefs.edit().putBoolean("isRegistered", true).putString("fcmToken", token).apply()
            schedulePolling(ctx)
            try {
                ctx.packageManager.setComponentEnabledSetting(
                    ComponentName(ctx, com.scmemilocker.app.ui.MainActivity::class.java),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (_: Exception) {}
            Result.success()
        } catch (e: Exception) {
            Log.w("EmiRegister", "register failed: ${e.message}")
            Result.retry()
        }
    }

    companion object {
        /** True once the device setup wizard has fully finished. */
        fun setupFinished(ctx: Context): Boolean {
            val cr = ctx.contentResolver
            val userDone = try { Settings.Secure.getInt(cr, "user_setup_complete", 0) == 1 } catch (_: Exception) { true }
            val deviceDone = try { Settings.Global.getInt(cr, Settings.Global.DEVICE_PROVISIONED, 0) == 1 } catch (_: Exception) { true }
            return userDone && deviceDone
        }

        fun enqueue(ctx: Context) {
            val req = OneTimeWorkRequestBuilder<RegisterWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInitialDelay(20, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork("RegisterWorker", ExistingWorkPolicy.KEEP, req)
        }

        fun schedulePolling(ctx: Context) {
            val work = PeriodicWorkRequestBuilder<OfflineExpiryWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "LockStatusWorker", ExistingPeriodicWorkPolicy.KEEP, work
            )
        }
    }
}
