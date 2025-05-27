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

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.numbux.ui.theme.NumbuxTheme




class PinActivity : ComponentActivity() {
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
        // allow edge-to-edge insets
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // 0) Si el teléfono está en lockscreen, no mostramos PIN: salimos
        val km = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (km.isKeyguardLocked) {
            finish()
            return
        }

        // 1) Leemos el paquete objetivo y elegimos PIN
        targetPkg = intent.getStringExtra("app_package")
        val appLockPin = PreferenceManager
            .getDefaultSharedPreferences(this)
            .getString("pin_app_lock", "1234") ?: "1234"
        correctPin = if (targetPkg in SYSTEM_PACKAGES) SYSTEM_PIN else appLockPin


        // 2) Now set Compose content:
                setContent {
                      NumbuxTheme {
                            PinScreen { entered ->
                                  if (entered == correctPin) {
                                       onPinCorrect()
                                     } else {
                                        Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                                      }
                                }
                          }
                    }

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
        super.onBackPressed()
    }

    @Composable
      private fun PinScreen(onSubmit: (String) -> Unit) {
            var credential by remember { mutableStateOf("") }
            var error     by remember { mutableStateOf<String?>(null) }
            // animate padding when ime appears
            val imePadding = WindowInsets.ime
              .asPaddingValues()
              .calculateBottomPadding()
              .let { it }
            val animatedBottom by animateDpAsState(targetValue = imePadding)

            Surface(
                  modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .padding(bottom = animatedBottom)
                        ) {
                  Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                              ) {
                        Text(
                              "Aplicación Bloqueada",
                              style = MaterialTheme.typography.headlineSmall
                                    )
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                              value = credential,
                              onValueChange = { new ->
                                    // only digits allowed
                                    if (new.all { it.isDigit() }) {
                                          credential = new
                                          error = null
                                        }
                                  },
                              label = { Text("PIN de Acceso") },
                              singleLine = true,
                              modifier = Modifier.fillMaxWidth(),
                              keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.NumberPassword,
                                    imeAction = ImeAction.Done
                                          ),
                              visualTransformation = PasswordVisualTransformation()
                                    )
                        error?.let {
                              Spacer(Modifier.height(8.dp))
                              Text(text = it, color = MaterialTheme.colorScheme.error)
                            }
                        Spacer(Modifier.height(24.dp))
                        Button(
                              onClick = {
                                    if (credential.isBlank()) {
                                          error = "Requerido"
                                        } else {
                                          onSubmit(credential.trim())
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


