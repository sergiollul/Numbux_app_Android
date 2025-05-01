package com.example.numbux

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.example.numbux.ui.theme.NumbuxTheme

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Comprueba si ya tenemos un rol guardado
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        when (prefs.getString("role", null)) {
            "controller" -> {
                startActivity(Intent(this, ControlActivity::class.java))
                finish()
                return
            }
            "student" -> {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                return
            }
        }

        // 2) Mostrar pantalla de login
        setContent {
            var credential by remember { mutableStateOf("") }
            var error      by remember { mutableStateOf<String?>(null) }

            NumbuxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier           = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Acceso Numbux", style = MaterialTheme.typography.headlineSmall)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value        = credential,
                            onValueChange= {
                                credential = it
                                error      = null
                            },
                            label        = { Text("Clave de acceso") },
                            singleLine   = true,
                            modifier     = Modifier.fillMaxWidth()
                        )
                        error?.let { e ->
                            Spacer(Modifier.height(8.dp))
                            Text(e, color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = {
                                when (credential.trim()) {
                                    "profesor1234" -> {
                                        // guardamos y arrancamos controlador
                                        prefs.edit()
                                            .putString("role", "controller")
                                            .apply()
                                        startActivity(Intent(this@LoginActivity, ControlActivity::class.java))
                                        finish()
                                    }
                                    "estudiante1234" -> {
                                        // guardamos y arrancamos bloqueador
                                        prefs.edit()
                                            .putString("role", "student")
                                            .apply()
                                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                        finish()
                                    }
                                    else -> {
                                        error = "Clave incorrecta"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Entrar")
                        }
                    }
                }
            }
        }
    }
}
