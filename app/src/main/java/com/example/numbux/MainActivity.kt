package com.example.numbux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.app.AlertDialog

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.preference.PreferenceManager

import com.example.numbux.accessibility.AppBlockerService
import com.example.numbux.ui.PinActivity
import com.example.numbux.ui.theme.NumbuxTheme
import com.example.numbux.utils.getDefaultLauncherPackage
import com.example.numbux.control.BlockManager

import android.widget.EditText
import android.text.InputType
import android.widget.Toast


import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, AppBlockerService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':').apply {
            setString(enabled)
        }
        return splitter.any { it.equals(expected, ignoreCase = true) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            finish() // stop here until they grant it
            return
        }

        enableEdgeToEdge()

        val launcher = getDefaultLauncherPackage(this)
        BlockManager.setBlockedAppsExcept(
            this,
            listOf(
                packageName,
                "com.android.settings",
                "com.android.systemui",
            ).let {
                if (launcher != null) it + launcher else it
            }
        )

        if (!isAccessibilityServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("Activar Accesibilidad")
                .setMessage(
                    "Para que Numbux funcione correctamente, " +
                            "ve a 'Apps instaladas'. Después 'Numbux' y actívalo."
                )
                .setPositiveButton("Entendido") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setCancelable(false)
                .show()
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        // capture the Activity for use inside the Compose lambda
        val activity = this

        setContent {
            var blockingEnabled by rememberSaveable {
                mutableStateOf(prefs.getBoolean("blocking_enabled", false))
            }

            NumbuxTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Numbux") },
                        )
                    }
                ) { innerPadding ->                               // <— only one lambda here!
                    MainScreen(
                        modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                        blockingEnabled = blockingEnabled,
                        onToggleBlocking = { enabled ->
                            if (!enabled) {
                                // build a PIN‐entry EditText
                                val pinInput = EditText(activity).apply {
                                    inputType = InputType.TYPE_CLASS_NUMBER or
                                            InputType.TYPE_NUMBER_VARIATION_PASSWORD
                                    hint = "####"
                                }

                                AlertDialog.Builder(activity)
                                .setTitle("Ingrese PIN para desactivar")
                                .setView(pinInput)
                                .setPositiveButton("OK") { dialog, _ ->
                                    val entered = pinInput.text.toString()
                                    val correct = prefs.getString("pin_app_lock", "1234")
                                    if (entered == correct) {
                                        // PIN OK → actually disable
                                        prefs.edit().putBoolean("blocking_enabled", false).apply()
                                        blockingEnabled = false
                                        } else {
                                        Toast.makeText(
                                            activity,
                                            "PIN incorrecto",
                                            Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                .setNegativeButton("Cancelar", null)
                                .show()
                            } else {
                                // turning ON: No PIN needed
                                prefs.edit().putBoolean("blocking_enabled", true).apply()
                                blockingEnabled = true
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    blockingEnabled: Boolean,
    onToggleBlocking: (Boolean) -> Unit
) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Bienvenido a Numbux", style = MaterialTheme.typography.headlineSmall)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bloqueo de apps")
            Switch(
                checked = blockingEnabled,
                onCheckedChange = onToggleBlocking
            )
        }
        Text(
            if (blockingEnabled) "El bloqueador está ACTIVADO"
            else                  "El bloqueador está DESACTIVADO",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NumbuxTheme {
        MainScreen(
            blockingEnabled = true,
            onToggleBlocking = { }
        )
    }
}
