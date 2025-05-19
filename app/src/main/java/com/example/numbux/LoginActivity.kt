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
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.widget.TextView
import android.content.res.ColorStateList
import android.graphics.drawable.StateListDrawable



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

    private fun showNotificationPermissionDialog() {
        // 1) Preparamos el título en negrita
        val titleSpannable = SpannableString("Permisos de Notificación").apply {
            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // 2) Preparamos el mensaje con NumbuX en negrita y "permiso" subrayado
        val messageBuilder = SpannableStringBuilder().apply {
            append("Para guiarte por el proceso de instalación, ")
            val nb = SpannableString("NumbuX")
            nb.setSpan(StyleSpan(Typeface.BOLD), 0, nb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(nb)
            append(" necesita ")
            val perm = SpannableString("permiso")
            perm.setSpan(UnderlineSpan(), 0, perm.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(perm)
            append(" en las notificaciones.\n\nPor favor, acéptalo. 🙂")
        }

        // 3) Construimos el diálogo
        val dialog = AlertDialog.Builder(this)
            .setTitle(titleSpannable)
            .setMessage(messageBuilder)
            .setPositiveButton("Vale") { _, _ ->
                prefs.edit().putBoolean(KEY_NOTIF_DIALOG_SHOWN, true).apply()
                requestNotificationPermission()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        // fondo del diálogo
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))

        // botón “Vale” con fondo blanco/negro y texto negro/blanco
        val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        val bgStates = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), ColorDrawable(Color.BLACK))
            addState(intArrayOf(), ColorDrawable(Color.WHITE))
        }
        positive.background = bgStates
        val textStates = ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_pressed),
                intArrayOf()
            ),
            intArrayOf(
                Color.WHITE,
                Color.BLACK
            )
        )
        positive.setTextColor(textStates)

        // ***forzar texto negro en título y mensaje***
        val titleId = resources.getIdentifier("alertTitle", "id", "android")
        (dialog.findViewById<TextView>(titleId))?.setTextColor(Color.BLACK)
        (dialog.findViewById<TextView>(android.R.id.message))?.setTextColor(Color.BLACK)
    }
}