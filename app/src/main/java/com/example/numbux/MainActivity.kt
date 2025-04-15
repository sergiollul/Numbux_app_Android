package com.example.numbux

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.numbux.ui.PinActivity
import com.example.numbux.ui.theme.NumbuxTheme
import com.example.numbux.utils.getDefaultLauncherPackage
import com.example.numbux.control.BlockManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
            return // ⚠️ detenemos para evitar continuar sin permiso
        }

        enableEdgeToEdge()
        Log.d("Numbux", "✅ MainActivity arrancó correctamente")

        // 👉 Bloqueo de apps al iniciar
        val launcherPackage = getDefaultLauncherPackage(this)
        val whitelist = mutableListOf(
            packageName,
            "com.android.settings",
            "com.android.systemui",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.inputmethod",
            "com.miui.securitycenter"
        )

        launcherPackage?.let {
            whitelist.add(it)
        }

        BlockManager.setBlockedAppsExcept(this, whitelist)
        Log.d("Numbux", "📋 Apps bloqueadas (desde MainActivity): ${BlockManager.getBlockedAppsDebug()}")

        // 🛡️ Vigilancia de accesibilidad
        if (!isAccessibilityServiceEnabled(this, "com.example.numbux.accessibility.AppBlockerService")) {
            Log.w("Numbux", "⚠️ Servicio de accesibilidad desactivado")

            val intent = Intent(this, PinActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("app_package", "accessibility_shutdown")
            }
            startActivity(intent)
        } else {
            Log.d("Numbux", "🟢 Accesibilidad activa")
        }

        setContent {
            NumbuxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    // ✅ Función de vigilancia de accesibilidad
    private fun isAccessibilityServiceEnabled(context: Context, serviceName: String): Boolean {
        val expectedComponentName = ComponentName(context, serviceName).flattenToString()
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        return splitter.any { it.equals(expectedComponentName, ignoreCase = true) }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Bienvenido a Numbux", style = MaterialTheme.typography.headlineSmall)
        Text("Este dispositivo está siendo monitoreado por los padres.")

        Button(onClick = {
            val intent = Intent(context, PinActivity::class.java)
            context.startActivity(intent)
        }) {
            Text("Abrir pantalla de PIN")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    NumbuxTheme {
        MainScreen()
    }
}
