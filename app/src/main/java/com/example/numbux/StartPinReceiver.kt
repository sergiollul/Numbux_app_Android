package com.example.numbux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.numbux.ui.PinActivity

class StartPinReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appPackage = intent?.getStringExtra("app_package")

        val newIntent = Intent(context, PinActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("app_package", appPackage)
        }
        try {
            context.startActivity(newIntent)
        } catch (e: Exception) {
        }
    }
}
