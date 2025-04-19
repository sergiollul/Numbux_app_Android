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

// ===== Compose + State imports =====
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

// ===== Preferences =====
import androidx.preference.PreferenceManager

import com.example.numbux.accessibility.AppBlockerService
import com.example.numbux.ui.PinActivity
import com.example.numbux.ui.theme.NumbuxTheme
import com.example.numbux.utils.getDefaultLauncherPackage
import com.example.numbux.control.BlockManager

class MainActivity : ComponentActivity() {

    // No-arg version for our internal check
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

        // —————— 1) Mandatory Android setup BEFORE Compose ——————

        // Overlay permission
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    android.net.Uri.parse("package:$packageName")
                )
            )
            finish() // stop here until they grant it
            return
        }

        // Edge‑to‑edge
        enableEdgeToEdge()
        Log.d("Numbux", "✅ MainActivity arrancó correctamente")

        // BlockManager initial whitelist
        val launcher = getDefaultLauncherPackage(this)
        BlockManager.setBlockedAppsExcept(this, listOf(
            packageName,
            "com.android.settings",
            "com.android.systemui",
            // …
        ).let {
            if (launcher != null) it + launcher else it
        })
        Log.d("Numbux", "📋 Apps bloqueadas: ${BlockManager.getBlockedAppsDebug()}")

        // Accessibility alert
        if (!isAccessibilityServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle("Activar Accesibilidad")
                .setMessage("Para que Numbux funcione correctamente, ve a 'Apps instaladas'. Después 'Numbux' y actívalo.")
                .setPositiveButton("Entendido") { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                .setCancelable(false)
                .show()
        } else {
            Log.d("Numbux", "🟢 Accesibilidad activa")
        }

        // —————— 2) Now call Compose once, and only Compose code inside ——————
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        setContent {
            // ① Compose state backed by prefs
            var blockingEnabled by rememberSaveable {
                mutableStateOf(prefs.getBoolean("blocking_enabled", true))
            }

            NumbuxTheme {
                Scaffold { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        blockingEnabled = blockingEnabled,
                        onToggleBlocking = { newValue ->
                            // 1️⃣ update Compose state & prefs
                            blockingEnabled = newValue
                            prefs.edit().putBoolean("blocking_enabled", newValue).apply()

                            if (newValue) {
                                // 🎉 Just turned blocking back ON → reset any old PIN overrides
                                BlockManager.clearAllDismissed()
                                BlockManager.clearAllTemporarilyAllowed()
                            }
                        }
                    )
                }
            }
        }
    }
}


// ————— Composable UI —————
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
            else                   "El bloqueador está DESACTIVADO",
            style = MaterialTheme.typography.bodyMedium
        )

        val ctx = LocalContext.current
        Button(onClick = {
            ctx.startActivity(Intent(ctx, PinActivity::class.java))
        }) {
            Text("Abrir pantalla de PIN")
        }
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
