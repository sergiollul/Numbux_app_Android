// src/main/java/com/example/numbux/WallpaperHelper.kt
package com.example.numbux

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.annotation.SuppressLint
import android.view.View
import java.io.File
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import android.view.Gravity
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import androidx.preference.PreferenceManager

object WallpaperHelper {

    /**
     * Sets both the home (SYSTEM) and lock wallpapers to
     * R.drawable.numbux_wallpaper_homelock, then recenters the
     * SYSTEM wallpaper so you never see it shifted to the right.
     *
     * This works from an Activity or from a Service (e.g. your
     * AccessibilityService) by temporarily adding an invisible
     * overlay to get a valid windowToken for recentering.
     */
    fun enableLockWallpaper(ctx: Context) {
        val bmp = BitmapFactory.decodeResource(
            ctx.resources,
            R.drawable.numbux_wallpaper_homelock
        )
        val wm = WallpaperManager.getInstance(ctx)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 1) Apply both SYSTEM and LOCK wallpapers
            wm.setBitmap(
                bmp,
                /* visibleCropHint= */ null,
                /* allowBackup= */ true,
                /* which= */ WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )

            // 2) Spin up an invisible 1×1 overlay just long enough to grab its token
            val overlay = View(ctx)
            val lp = LayoutParams(
                1, 1,
                // requires SYSTEM_ALERT_WINDOW permission (you already check canDrawOverlays)
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LayoutParams.FLAG_NOT_FOCUSABLE
                        or LayoutParams.FLAG_NOT_TOUCHABLE
                        or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }
            val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

            windowManager.addView(overlay, lp)

            // 3) Esperar a que el overlay reciba su windowToken, y solo entonces
            overlay.post {
                overlay.windowToken?.let { token ->
                    wm.setWallpaperOffsets(token, 0.5f, 0.5f)
                    wm.setWallpaperOffsetSteps(1f, 1f)
                }
                // 4) Desmontar el overlay
                try {
                    windowManager.removeView(overlay)
                } catch (e: Exception) {
                    // por si ya fue removido o no está agregado
                }
            }
        } else {
            // Pre-Nougat: single-wallpaper API
            wm.setBitmap(bmp)
        }
    }

    @SuppressLint("NewApi")
    fun restoreOriginalWallpapers(ctx: Context) {

        // --- MARCO ESTE RESTORE COMO INTERNO (para que el BroadcastReceiver lo ignore)
            val prefs = PreferenceManager.getDefaultSharedPreferences(ctx)
            prefs.edit()
                .putLong("last_internal_wallpaper_change", System.currentTimeMillis())
                .apply()
            // ---------------------------------------------------------------

        val wm = WallpaperManager.getInstance(ctx)
        val wmngr = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 1) Restore HOME as you already have
            File(ctx.filesDir, "wallpaper_backup_home.png")
                .takeIf { it.exists() }
                ?.let { f ->
                    val homeBmp = BitmapFactory.decodeFile(f.absolutePath)
                    wm.setBitmap(homeBmp)
                    // tiny overlay recenter…
                    val overlay = View(ctx.applicationContext)
                    val lp = LayoutParams(
                        1, 1,
                        LayoutParams.TYPE_APPLICATION_OVERLAY,
                        LayoutParams.FLAG_NOT_FOCUSABLE or
                                LayoutParams.FLAG_NOT_TOUCHABLE or
                                LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    ).apply { gravity = Gravity.TOP or Gravity.START }
                    wmngr.addView(overlay, lp)
                    wm.setWallpaperOffsets(overlay.windowToken, 0.5f, 0.5f)
                    wm.setWallpaperOffsetSteps(1f, 1f)
                    wmngr.removeView(overlay)
                }

            // 2) Now restore LOCK
            File(ctx.filesDir, "wallpaper_backup_lock.png")
                .takeIf { it.exists() }
                ?.let { f ->

            // marco también justo antes de aplicar el lock
            prefs.edit()
                .putLong("last_internal_wallpaper_change", System.currentTimeMillis())
                .apply()
            val lockBmp = BitmapFactory.decodeFile(f.absolutePath)
            wm.setBitmap(
                    lockBmp,
                    /* visibleCropHint= */ null,
                    /* allowBackup= */ true,
                    /* which= */ WallpaperManager.FLAG_LOCK
                        )

                }
        } else {
            // Pre-Nougat single‐wallpaper API
            File(ctx.filesDir, "wallpaper_backup_home.png")
                .takeIf { it.exists() }
                ?.let { f ->
                    wm.setBitmap(BitmapFactory.decodeFile(f.absolutePath))
                }
        }
    }
}