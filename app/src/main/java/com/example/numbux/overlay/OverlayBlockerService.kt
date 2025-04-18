package com.example.numbux.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import com.example.numbux.R

class OverlayBlockerService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // ✅ Verificación de permiso de superposición
        if (!android.provider.Settings.canDrawOverlays(this)) {
            Log.e("OverlayBlocker", "🚫 No tengo permiso para mostrar overlays")
            stopSelf()
            return START_NOT_STICKY
        }

        Log.d("OverlayBlocker", "🛡️ Creando overlay de pantalla completa")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_blocker, null)

        // 👇 Bloquea todos los toques
        overlayView?.setOnTouchListener { _, _ ->
            Log.d("OverlayBlocker", "🚫 Toque bloqueado por overlay")
            true
        }
        overlayView?.isClickable = true
        overlayView?.isFocusable = true

        // 🔴 Ayuda visual para pruebas (puedes comentar esto en producción)
        overlayView?.setBackgroundColor(0x55FF0000) // rojo semi-transparente

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e("OverlayBlocker", "💥 Error al añadir overlay: ${e.message}")
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager.removeView(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
