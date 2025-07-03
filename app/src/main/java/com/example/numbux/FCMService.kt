package com.example.numbux

import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(msg: RemoteMessage) {
        // 1) Read the payload
        msg.data["toggle"]?.toBoolean()?.let { enabled ->
            // 2) Persist exactly the same key your MainActivity reads:
            val prefs: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(applicationContext)
            prefs.edit().putBoolean("blocking_enabled", enabled).apply()

            // 3) If your UI is in the foreground, notify it:
            LocalBroadcastManager.getInstance(this).sendBroadcast(
                Intent("blocking-toggle-changed")
                    .putExtra("enabled", enabled)
            )
        }
    }
}
