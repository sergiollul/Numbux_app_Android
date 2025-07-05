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

            // 3) Recenter the wallpaper using the overlay's token
            val token: IBinder = overlay.windowToken
            wm.setWallpaperOffsets(token, 0.5f, 0.5f)
            wm.setWallpaperOffsetSteps(1f, 1f)

            // 4) Tear down the overlay immediately
            windowManager.removeView(overlay)

        } else {
            // Pre-Nougat: single-wallpaper API
            wm.setBitmap(bmp)
        }
    }

    @SuppressLint("NewApi")
    fun restoreOriginalWallpapers(ctx: Context) {
        val wm = WallpaperManager.getInstance(ctx)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // ─── 1) Restore HOME wallpaper ───────────────────────────────
            File(ctx.filesDir, "wallpaper_backup_home.png")
                .takeIf { it.exists() }
                ?.also { f ->
                    val homeBmp = BitmapFactory.decodeFile(f.absolutePath)
                    // ← this one-argument call recenters for you
                    wm.setBitmap(homeBmp)
                }

            // ─── 2) (Optional) tiny overlay + recenter to reset parallax ──
            Handler(Looper.getMainLooper()).postDelayed({
                val overlay = View(ctx)
                val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_PHONE

                val lp = WindowManager.LayoutParams(
                    1, 1, type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                            or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply { gravity = Gravity.TOP or Gravity.START }

                val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.addView(overlay, lp)
                wm.setWallpaperOffsets(overlay.windowToken, 0.5f, 0.5f)
                wm.setWallpaperOffsetSteps(1f, 1f)
                windowManager.removeView(overlay)
            }, 100L)

            // ─── 3) Restore LOCK wallpaper (no crop needed) ──────────────
            File(ctx.filesDir, "wallpaper_backup_lock.png")
                .takeIf { it.exists() }
                ?.also { f ->
                    val lockBmp = BitmapFactory.decodeFile(f.absolutePath)
                    wm.setBitmap(
                        lockBmp,
                        /* visibleCropHint= */ null,
                        /* allowBackup= */ true,
                        /* which= */ WallpaperManager.FLAG_LOCK
                    )
                }

        } else {
            // Pre-Nougat: single-wallpaper API
            File(ctx.filesDir, "wallpaper_backup_home.png")
                .takeIf { it.exists() }
                ?.also { f ->
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    wm.setBitmap(bmp)
                }
        }
    }
}
