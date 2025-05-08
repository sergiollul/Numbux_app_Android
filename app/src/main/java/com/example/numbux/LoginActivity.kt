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
import androidx.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.numbux.ui.theme.NumbuxTheme
import com.example.numbux.utils.AccessibilityUtils
import com.example.numbux.notifications.NotificationUtils
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearOutSlowInEasing



class LoginActivity : ComponentActivity() {

    companion object {
        private const val NOTIF_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make the window resize when the keyboard appears
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        // 1) Check saved role
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
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

        // 2) Show login UI
        setContent {
            // — state for the text field & error —
            var credential by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }

            // — compute & animate IME inset —
            val density = LocalDensity.current
            val imeBottomPx = WindowInsets.ime.getBottom(density)
            val imeBottomDp = with(density) { imeBottomPx.toDp() }
            val animatedImeDp by animateDpAsState(
                targetValue = imeBottomDp,
                animationSpec = tween(
                    durationMillis = 30_000,             // <-- 30s instead of 300ms
                    easing = LinearOutSlowInEasing      // optional smoother easing
                )
            )

            NumbuxTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .padding(bottom = animatedImeDp) // smooth slide up
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
                            onValueChange = { new ->
                                credential = new
                                error = null
                            },
                            label = { Text("Clave de acceso") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        error?.let { e ->
                            Spacer(Modifier.height(8.dp))
                            Text(text = e, color = MaterialTheme.colorScheme.error)
                        }

                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { handleLogin(credential.trim(), prefs) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Entrar")
                        }
                    }
                }
            }
        }
    }

    private fun handleLogin(
        credential: String,
        prefs: android.content.SharedPreferences
    ) {
        when (credential) {
            "profesor1234" -> {
                prefs.edit().putString("role", "controller").apply()
                navigateTo(ControlActivity::class.java)
            }
            "estudiante1234" -> {
                prefs.edit().putString("role", "student").apply()
                requestOrShowNotification()
            }
            else -> {
                // e.g. set error state if you want:
                // error = "Clave incorrecta"
            }
        }
    }

    private fun requestOrShowNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_REQUEST_CODE
            )
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
}
