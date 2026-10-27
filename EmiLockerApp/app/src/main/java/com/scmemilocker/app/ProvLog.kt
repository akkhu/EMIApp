package com.scmemilocker.app

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Fire-and-forget diagnostics: reports which setup step the phone reached to
 * /api/provlog, so provisioning failures can be located without USB/logcat.
 * Never blocks or throws.
 */
object ProvLog {
    private const val URL = "https://emi-app-7pq1.vercel.app/api/provlog"
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    fun send(ctx: Context, stage: String, detail: String = "") {
        Log.i("EmiProvLog", "$stage $detail")
        val version = try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
        Thread {
            try {
                val json = Gson().toJson(mapOf(
                    "stage" to stage,
                    "detail" to detail,
                    "model" to (Build.MANUFACTURER + " " + Build.MODEL),
                    "sdk" to Build.VERSION.SDK_INT,
                    "version" to version
                ))
                client.newCall(
                    Request.Builder().url(URL)
                        .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()
                ).execute().close()
            } catch (_: Exception) {}
        }.start()
    }
}
