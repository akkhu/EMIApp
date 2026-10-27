package com.scmemilocker.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Earliest code that runs in our process (attachBaseContext runs before any
 * library initializer). Reports "process_start" and sends any crash's stack
 * trace to /api/provlog, so setup failures can be diagnosed without USB.
 */
class EmiApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val sw = StringWriter(); e.printStackTrace(PrintWriter(sw))
                postBlocking("crash", "thread=${t.name} " + sw.toString().take(3500))
            } catch (_: Throwable) {}
            previous?.uncaughtException(t, e)
        }
        Thread { postBlocking("process_start", "") }.start()
    }

    companion object {
        /** Plain HttpURLConnection so it works even if other libraries failed to load. */
        fun postBlocking(stage: String, detail: String) {
            try {
                val body = org.json.JSONObject()
                    .put("stage", stage)
                    .put("detail", detail)
                    .put("model", Build.MANUFACTURER + " " + Build.MODEL)
                    .put("sdk", Build.VERSION.SDK_INT)
                    .put("version", "2.5-poc")
                    .toString()
                val c = URL("https://emi-app-7pq1.vercel.app/api/provlog").openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 4000; c.readTimeout = 4000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
                c.responseCode
                c.disconnect()
            } catch (e: Throwable) {
                Log.w("EmiApp", "provlog post failed: ${e.message}")
            }
        }
    }
}
