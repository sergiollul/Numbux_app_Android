package com.example.numbux

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.numbux.ui.theme.NumbuxTheme
import com.example.numbux.utils.AccessibilityUtils
import com.example.numbux.notifications.NotificationUtils

class LoginActivity : ComponentActivity() {

    companion object {
        private const val NOTIF_REQUEST_CODE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Comprueba si ya tenemos un rol guardado
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        when (prefs.getString("role", null)) {
            "controller" -> {
                navigateTo(ControlActivity::class.java)
                return
            }
            "student" -> {
                // Si ya tiene rol de estudiante y accesibilidad activa, cancelar notificación
                if (AccessibilityUtils.isAccessibilityEnabled(this)) {
                    NotificationUtils.cancelAccessibilityNotification(this)
                }
                navigateTo(MainActivity::class.java)
                return
            }
        }

        // 2) Mostrar la pantalla de login
        setContent {
            var credential by remember { mutableStateOf("") }
            var error by remember { mutableStateOf<String?>(null) }

            NumbuxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Introduce tu usuario", style = MaterialTheme.typography.headlineSmall)
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
                        error?.let { e ->
                            Spacer(Modifier.height(8.dp))
                            Text(e, color = MaterialTheme.colorScheme.error)
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

    private fun handleLogin(credential: String, prefs: android.content.SharedPreferences) {
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
                // Aquí podrías mostrar un error de UI; en este diseño se ignora
            }
        }
    }

    private fun requestOrShowNotification() {
        // Para Android 13+ pedimos permiso POST_NOTIFICATIONS
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
            // Si no hay accesibilidad activada, mostramos notificación fija
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