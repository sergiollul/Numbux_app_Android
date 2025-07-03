// src/main/java/com/example/numbux/WallpaperHelper.kt
package com.example.numbux

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import java.io.File
import android.annotation.SuppressLint

object WallpaperHelper {

    fun enableLockWallpaper(ctx: Context) {
        // load your “locked” wallpaper from resources
        val bmp = BitmapFactory.decodeResource(ctx.resources, R.drawable.numbux_wallpaper_homelock)
        val wm  = WallpaperManager.getInstance(ctx)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_SYSTEM)
            wm.setBitmap(bmp, null, true, WallpaperManager.FLAG_LOCK)
        } else {
            wm.setBitmap(bmp)
        }
    }
    @SuppressLint("NewApi")
    fun restoreOriginalWallpapers(ctx: Context) {
        val wm = WallpaperManager.getInstance(ctx)

        // restore home
        File(ctx.filesDir, "wallpaper_backup_home.png").takeIf { it.exists() }?.also { f ->
            val homeBmp = BitmapFactory.decodeFile(f.absolutePath)
            wm.setBitmap(homeBmp, null, true, WallpaperManager.FLAG_SYSTEM)
        }
        // restore lock
        File(ctx.filesDir, "wallpaper_backup_lock.png").takeIf { it.exists() }?.also { f ->
            val lockBmp = BitmapFactory.decodeFile(f.absolutePath)
            wm.setBitmap(lockBmp, null, true, WallpaperManager.FLAG_LOCK)
        }
    }
}
