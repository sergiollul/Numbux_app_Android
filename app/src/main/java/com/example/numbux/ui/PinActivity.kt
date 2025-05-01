package com.example.numbux.ui

import android.app.Activity
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

class PinActivity : Activity() {

    companion object {
        // Packages requiring the fixed system PIN
        private val SYSTEM_PACKAGES = setOf(
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.android.settings"
        )
        private const val SYSTEM_PIN = "5678"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin)

        // Obtain SharedPreferences
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        // Get the package name we need to unlock
        val targetPkg: String? = intent.getStringExtra("app_package")

        // Get the user-defined app-lock PIN (default 1234)
        val appLockPin: String = prefs.getString("pin_app_lock", "1234") ?: "1234"
        // Decide which PIN to validate
        val correctPin: String = if (targetPkg != null && SYSTEM_PACKAGES.contains(targetPkg)) {
            SYSTEM_PIN
        } else {
            appLockPin
        }

        // Setup UI elements
        val input = findViewById<EditText>(R.id.editTextPin)
        val btn = findViewById<Button>(R.id.buttonUnlock)

        btn.setOnClickListener {
            if (input.text.toString() == correctPin) {
                targetPkg?.let { pkg ->
                    // Mark as temporarily allowed
                    BlockManager.allowTemporarily(pkg)
                    Toast.makeText(this, "Desbloqueado: $pkg", Toast.LENGTH_SHORT).show()
                }
                setResult(Activity.RESULT_OK)
                BlockManager.isShowingPin = false

                // only now do we “dismiss until the app changes”
                intent.getStringExtra("app_package")?.let { pkg ->
                    BlockManager.dismissUntilAppChanges(pkg)
                }

                finish()
            } else {
                Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
            }
        }

        // Keep screen on and override lock
        window.addFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
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
