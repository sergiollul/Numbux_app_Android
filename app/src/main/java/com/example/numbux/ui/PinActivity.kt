package com.example.numbux.ui

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.view.MotionEvent
import android.view.WindowManager
import android.graphics.Rect
import android.view.View
import androidx.preference.PreferenceManager
import com.example.numbux.R
import com.example.numbux.control.BlockManager
import android.app.KeyguardManager
import android.content.Context

class PinActivity : Activity() {
    companion object {
        private val SYSTEM_PACKAGES = setOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.android.settings"
        )
        private const val SYSTEM_PIN = "5678"
    }

    private var targetPkg: String? = null
    private lateinit var correctPin: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 0) Si el teléfono está en lockscreen, no mostramos PIN: salimos
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) {
            finish()
            return
        }

        // 1) Inflamos la UI
        setContentView(R.layout.activity_pin)

        // 2) Leemos el paquete objetivo y elegimos PIN
        targetPkg = intent.getStringExtra("app_package")
        val appLockPin = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString("pin_app_lock", "1234") ?: "1234"
        correctPin = if (targetPkg in SYSTEM_PACKAGES) SYSTEM_PIN else appLockPin

        // 3) Botón de desbloqueo
        findViewById<Button>(R.id.buttonUnlock).setOnClickListener {
            val entered = findViewById<EditText>(R.id.editTextPin).text.toString()
            if (entered == correctPin) onPinCorrect()
            else Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
        }

        // 4) Quitamos las flags SHOW_WHEN_LOCKED / DISMISS_KEYGUARD
        //    (Ahora la Activity solo aparece si ya desbloqueaste la pantalla)
        //    window.addFlags(
        //      WindowManager.LayoutParams.FLAG_FULLSCREEN or
        //      WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        //    )
    }


    private fun onPinCorrect() {
        // stop the service from re-showing PIN
        BlockManager.isShowingPin = false

        if (targetPkg != null && SYSTEM_PACKAGES.contains(targetPkg)) {
            // SYSTEM flow covers both: uninstall *and* disable‐accessibility
            // 1) allow that package once
            BlockManager.allowTemporarily(targetPkg!!)
            Toast.makeText(this, "Desbloqueado", Toast.LENGTH_SHORT).show()
            // 2) simply dismiss the PIN screen—underneath stays the installer or the Settings dialog
            finish()
        } else if (targetPkg != null) {
            // normal app‐lock flow
            BlockManager.allowTemporarily(targetPkg!!)
            Toast.makeText(this, "Desbloqueado: $targetPkg", Toast.LENGTH_SHORT).show()
            setResult(Activity.RESULT_OK)
            BlockManager.dismissUntilAppChanges(targetPkg!!)
            packageManager.getLaunchIntentForPackage(targetPkg!!)?.also { launch ->
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(launch)
            }
            finish()
        } else {
            finish()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        val pinLayout = findViewById<View>(R.id.pinLayoutRoot)
        if (pinLayout != null && ev != null) {
            val rect = Rect()
            pinLayout.getGlobalVisibleRect(rect)
            val x = ev.rawX.toInt()
            val y = ev.rawY.toInt()
            if (rect.contains(x, y)) {
                return super.dispatchTouchEvent(ev)
            }
            // ignore touches outside
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onPause() {
        super.onPause()
        BlockManager.isShowingPin = false
    }

    override fun onDestroy() {
        super.onDestroy()
        BlockManager.isShowingPin = false
    }

    override fun onBackPressed() {
        // disable back button
    }
}

