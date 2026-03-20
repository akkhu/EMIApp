package com.emilocker.app.ui

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.emilocker.app.R

class LockedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_locked)

        // Lock the task, meaning the user cannot use recent apps or home buttons to leave.
        startLockTask()

        findViewById<Button>(R.id.btnCheckStatus).setOnClickListener {
            // In a real app, this would ping the API. For now, just show a toast.
            Toast.makeText(this, "Checking payment status...", Toast.LENGTH_SHORT).show()
        }
    }

    // Modern replacement for onBackPressed() handling is via OnBackPressedDispatcher
    // But overriding the method to do nothing is a simple lock mechanism
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Do nothing to prevent exiting the lock screen
    }
}
