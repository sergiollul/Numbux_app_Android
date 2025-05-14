// WallpaperChangeReceiver.kt
package com.example.numbux

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class WallpaperChangeReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_WALLPAPER_CHANGED) return

        val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
        val lastInternal = prefs.getLong("last_internal_wallpaper_change", 0L)
        val now = System.currentTimeMillis()

        // if the wallpaper was changed by our own code in the last 2 s, ignore it
        if (now - lastInternal < 2_000) return

        // genuine external change → prompt on next launch/resume
        prefs.edit()
            .putBoolean("backup_prompt_pending", true)
            .apply()
    }
}

