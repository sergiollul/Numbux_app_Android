package com.example.numbux

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.annotation.SuppressLint
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams
import java.io.File

object WallpaperHelper {

    /**
     * Applies both SYSTEM and LOCK wallpapers and recenters SYSTEM
     * by using a temporary overlay and setWallpaperOffsets.
     */
    @SuppressLint("NewApi")
    fun enableLockWallpaper(ctx: Context) {
        val bmp = BitmapFactory.decodeResource(
            ctx.resources,
            R.drawable.numbux_wallpaper_homelock
        )
        val wm = WallpaperManager.getInstance(ctx)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Apply both SYSTEM & LOCK
            wm.setBitmap(
                bmp,
                /* visibleCropHint= */ null,
                /* allowBackup= */ true,
                /* which= */ WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
            )

            // Temporary overlay to recenter
            val overlay = View(ctx.applicationContext)
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                LayoutParams.TYPE_PHONE
            }
            val lp = LayoutParams(
                1, 1,
                type,
                LayoutParams.FLAG_NOT_FOCUSABLE
                        or LayoutParams.FLAG_NOT_TOUCHABLE
                        or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.TOP or Gravity.START }

            val wmngr = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wmngr.addView(overlay, lp)

            // Recenter wallpaper offsets
            val token = overlay.windowToken
            wm.setWallpaperOffsets(token, 0.5f, 0.5f)
            wm.setWallpaperOffsetSteps(1f, 1f)

            wmngr.removeView(overlay)
        } else {
            // Pre-N: simple centered set
            wm.setBitmap(bmp)
        }
    }

    /**
     * Restores HOME and LOCK wallpapers from backups,
     * centering HOME via one-arg setBitmap + overlay recenter.
     */
    @SuppressLint("NewApi")
    fun restoreOriginalWallpapers(ctx: Context) {
        val wm = WallpaperManager.getInstance(ctx)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Restore HOME (centers automatically)
            File(ctx.filesDir, "wallpaper_backup_home.png")
                .takeIf { it.exists() }
                ?.let { f ->
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    wm.setBitmap(bmp)

                    // Overlay recenter to clear any parallax
                    val overlay = View(ctx.applicationContext)
                    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        @Suppress("DEPRECATION")
                        LayoutParams.TYPE_PHONE
                    }
                    val lp = LayoutParams(
                        1, 1,
                        type,
                        LayoutParams.FLAG_NOT_FOCUSABLE
                                or LayoutParams.FLAG_NOT_TOUCHABLE
                                or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    ).apply { gravity = Gravity.TOP or Gravity.START }

                    val wmngr2 = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                    wmngr2.addView(overlay, lp)
                    val token = overlay.windowToken
                    wm.setWallpaperOffsets(token, 0.5f, 0.5f)
                    wm.setWallpaperOffsetSteps(1f, 1f)
                    wmngr2.removeView(overlay)
                }

            // Restore LOCK (no recenter needed)
            File(ctx.filesDir, "wallpaper_backup_lock.png")
                .takeIf { it.exists() }
                ?.let { f ->
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    wm.setBitmap(
                        bmp,
                        /* visibleCropHint= */ null,
                        /* allowBackup= */ true,
                        /* which= */ WallpaperManager.FLAG_LOCK
                    )
                }
        } else {
            // Pre-N: simple restore
            File(ctx.filesDir, "wallpaper_backup_home.png")
                .takeIf { it.exists() }
                ?.let { f ->
                    val bmp = BitmapFactory.decodeFile(f.absolutePath)
                    wm.setBitmap(bmp)
                }
        }
    }
}
