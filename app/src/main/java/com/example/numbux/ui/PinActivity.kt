package com.example.numbux.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.numbux.R
import com.example.numbux.control.BlockManager
import android.util.Log
import android.content.Intent
import android.view.MotionEvent // ← Required for dispatchTouchEvent
import android.view.WindowManager
import android.graphics.Rect
import android.view.View
import com.example.numbux.overlay.OverlayBlockerService




class PinActivity : Activity() {

    private val correctPin = "1234" // Puedes vincular esto a Firebase después

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        val input = findViewById<EditText>(R.id.editTextPin)
        val btn = findViewById<Button>(R.id.buttonUnlock)
        val appPackage = intent.getStringExtra("app_package")

        btn.setOnClickListener {
            if (input.text.toString() == correctPin) {
                if (!appPackage.isNullOrEmpty()) {
                    BlockManager.allowTemporarily(appPackage)
                    Toast.makeText(this, "App desbloqueada temporalmente", Toast.LENGTH_SHORT).show()
                }

                // ✅ Apagar el overlay al ingresar el PIN correcto
                stopService(Intent(this, com.example.numbux.overlay.OverlayBlockerService::class.java))

                setResult(Activity.RESULT_OK)
                BlockManager.isShowingPin = false
                finish()
            } else {
                Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ Prevent interaction with buttons behind the dialog
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    // ✅ This consumes all touch events so nothing passes through
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val pinLayout = findViewById<View>(R.id.pinLayoutRoot) // <- your main container
        if (pinLayout != null) {
            val rect = Rect()
            pinLayout.getGlobalVisibleRect(rect)
            if (rect.contains(ev?.rawX?.toInt() ?: 0, ev?.rawY?.toInt() ?: 0)) {
                // Touch is inside the PIN UI, allow it
                return super.dispatchTouchEvent(ev)
            } else {
                // Touch is outside, consume it
                Log.d("Numbux", "❌ Tap fuera del PIN bloqueado")
                return true
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onPause() {
        super.onPause()
        Log.d("Numbux", "🛑 PinActivity -> onPause")
        BlockManager.isShowingPin = false

        // 🧹 Asegurarnos de que no quede ningún overlay bloqueando toques
        stopService(Intent(this, OverlayBlockerService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Numbux", "💀 PinActivity -> onDestroy")
        BlockManager.isShowingPin = false
        stopService(Intent(this, OverlayBlockerService::class.java))

        val appPackage = intent.getStringExtra("app_package")
        if (!appPackage.isNullOrEmpty()) {
            BlockManager.dismissUntilAppChanges(appPackage)
            Log.d("Numbux", "❌ PIN rechazado para $appPackage (temporalmente ignorado)")
        }
    }

    override fun onBackPressed() {
        // No permitir cerrar con back
    }
}