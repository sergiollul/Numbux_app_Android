package com.example.numbux

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.numbux.ui.theme.NumbuxTheme
import com.example.numbux.utils.AccessibilityUtils
import com.example.numbux.notifications.NotificationUtils
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.LinearOutSlowInEasing
import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.annotation.SuppressLint
import android.view.Gravity
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.FrameLayout


class LoginActivity : ComponentActivity() {

    private lateinit var prefs: SharedPreferences

    companion object {
        private const val NOTIF_REQUEST_CODE = 1002
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_NOTIF_DIALOG_SHOWN = "notif_dialog_shown"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        when (prefs.getString("role", null)) {
            "controller" -> {
                navigateTo(ControlActivity::class.java)
                return
            }
            "student" -> {
                if (AccessibilityUtils.isAccessibilityEnabled(this)) {
                    NotificationUtils.cancelAccessibilityNotification(this)
                }
                navigateTo(MainActivity::class.java)
                return
            }
        }

        setContent {
            var credential by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }

            val density = LocalDensity.current
            val imeBottomPx = WindowInsets.ime.getBottom(density)
            val imeBottomDp = with(density) { imeBottomPx.toDp() }
            val animatedImeDp by animateDpAsState(
                targetValue = imeBottomDp,
                animationSpec = tween(
                    durationMillis = 300,
                    easing = LinearOutSlowInEasing
                )
            )

            NumbuxTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .padding(bottom = animatedImeDp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Introduce tu usuario",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = credential,
                            onValueChange = {
                                credential = it
                                error = null
                            },
                            label = { Text("Clave de acceso") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        error?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { handleLogin(credential.trim()) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Entrar")
                        }
                    }
                }
            }
        }
    }

    private fun handleLogin(credential: String) {
        when (credential) {
            "profesor1234" -> {
                prefs.edit().putString("role", "controller").apply()
                navigateTo(ControlActivity::class.java)
            }
            "estudiante1234" -> {
                prefs.edit().putString("role", "student").apply()
                checkNotificationPermission()
            }
            else -> {
                // Opción: mostrar error
            }
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (!prefs.getBoolean(KEY_NOTIF_DIALOG_SHOWN, false)) {
                showNotificationPermissionDialog()
            } else {
                requestNotificationPermission()
            }
        } else {
            if (!AccessibilityUtils.isAccessibilityEnabled(this)) {
                NotificationUtils.showPersistentAccessibilityNotification(this)
            }
            navigateTo(MainActivity::class.java)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIF_REQUEST_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (!AccessibilityUtils.isAccessibilityEnabled(this)) {
                NotificationUtils.showPersistentAccessibilityNotification(this)
            }
        }
        navigateTo(MainActivity::class.java)
    }

    private fun navigateTo(target: Class<*>) {
        startActivity(Intent(this, target))
        finish()
    }

    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            NOTIF_REQUEST_CODE
        )
    }

    @SuppressLint("NewApi")
    private fun showNotificationPermissionDialog() {
        // 1) Inflate our custom dialog layout
        val customView = layoutInflater.inflate(
            R.layout.dialog_enable_notification,
            null,
            false
        )

        // 2) Build & show the dialog with our NoShadow style
        val dlg = AlertDialog.Builder(this, R.style.NoShadowDialog)
            .setView(customView)
            .setCancelable(false)
            .show()

        // 3) Make its background transparent
        dlg.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 4) Wait until the layout has been measured
        customView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                customView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                val dialogHeightPx = customView.height

                // 5) Create the “Entendido” button entirely in code
                val btn = Button(this@LoginActivity).apply {
                    text = "Entendido"
                    background = ContextCompat.getDrawable(context, R.drawable.button_bg_pressed_selector)
                    setTextColor(ContextCompat.getColorStateList(context, R.color.button_text_pressed_selector))
                    val pad = (16 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad / 2, pad, pad / 2)
                    setOnClickListener {
                       // 1) Dismiss the dialog…
                       dlg.dismiss()

                       // 2) Remember we showed it so next time we go straight to the system permission
                       prefs.edit()
                           .putBoolean(KEY_NOTIF_DIALOG_SHOWN, true)
                           .apply()

                       // 3) *Now* fire off the real POST_NOTIFICATIONS permission request
                       requestNotificationPermission()
                   }
                }

                // 6) Inject it just below the dialog’s content
                dlg.window
                    ?.decorView
                    ?.findViewById<FrameLayout>(android.R.id.content)
                    ?.let { container ->
                        val topMarginPx = dialogHeightPx + (8 * resources.displayMetrics.density).toInt()
                        val params = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
                            this.topMargin = topMarginPx
                        }
                        container.addView(btn, params)
                    }
            }
        })
    }
}