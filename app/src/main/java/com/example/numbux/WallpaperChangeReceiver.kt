// WallpaperChangeReceiver.kt
package com.example.numbux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class WallpaperChangeReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_WALLPAPER_CHANGED) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            prefs.edit()
                .putBoolean("backup_prompt_pending", true)
                .apply()
        }
    }
}
