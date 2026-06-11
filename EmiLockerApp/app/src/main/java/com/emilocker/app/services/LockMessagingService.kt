package com.emilocker.app.services

import android.util.Log
import com.emilocker.app.DeviceRestrictionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class LockMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // If already registered, we should ideally update the backend with the new token.
        // For MVP, we fetch the token during registration in MainActivity.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val restrictionManager = DeviceRestrictionManager(this)
        
        // Data payload contains the lock instruction
        val action = remoteMessage.data["action"]
        Log.d("FCM", "Message received, action: $action")

        if (action == "LOCK") {
            restrictionManager.enforceEmiLock()
        } else if (action == "UNLOCK") {
            restrictionManager.clearEmiLock()
        } else if (action == "RELEASE") {
            restrictionManager.releaseDevice()
        }
    }
}
