package com.example.numbux.overlay

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.FrameLayout
import android.util.Log
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

        Log.d("OverlayBlocker", "🛡️ Creando overlay")

        // 👉 LOGS para depurar coordenadas recibidas
        val x = intent?.getIntExtra("x", 200) ?: 200
        val y = intent?.getIntExtra("y", 600) ?: 600
        val width = intent?.getIntExtra("width", 300) ?: 300
        val height = intent?.getIntExtra("height", 100) ?: 100

        // 🪵 Log extra para confirmar coordenadas y visibilidad
        Log.d("Overlay", "🛑 Añadiendo overlay en x=$x y=$y w=$width h=$height")
        Log.d("OverlayBlocker", "🧩 Overlay en posición: x=$x y=$y w=$width h=$height")

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        overlayView = inflater.inflate(R.layout.overlay_blocker, null)

        val params = WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = x
        params.y = y

        overlayView?.setOnTouchListener { _, _ ->
            Log.d("OverlayBlocker", "🚫 Toque bloqueado por overlay")
            true // bloquea el toque
        }

        // 🔴 Ayuda visual para pruebas (comenta cuando termines)
        overlayView?.setBackgroundColor(0x55FF0000) // rojo semi-transparente

        windowManager.addView(overlayView, params)

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
