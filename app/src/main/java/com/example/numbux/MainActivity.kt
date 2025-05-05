package com.example.numbux

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.example.numbux.accessibility.AppBlockerService
import com.example.numbux.control.BlockManager
import com.example.numbux.ui.PinActivity
import com.example.numbux.ui.theme.NumbuxTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.WindowCompat

import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

import android.content.Context

import android.view.accessibility.AccessibilityManager
import android.accessibilityservice.AccessibilityServiceInfo





@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    companion object {
        private const val REQ_OVERLAY = 1001
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var blockingState: MutableState<Boolean>
    private lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener
    private var accessibilityDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Overlay‐permission check (unchanged)…
        if (!Settings.canDrawOverlays(this)) {
            startActivityForResult(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
                REQ_OVERLAY
            )
            return
        }

        // 2) Accessibility-service check
        if (!isAccessibilityServiceEnabled(this)) {
            showEnableAccessibilityDialog()
        }

        // 3) Initialize SharedPreferences & Compose state
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        blockingState = mutableStateOf(prefs.getBoolean("blocking_enabled", false))

        // 4) Listen for external prefs changes
        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { sp, key ->
            if (key == "blocking_enabled") {
                // update our Compose state
                blockingState.value = sp.getBoolean(key, false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // 5) Firebase remote listener (unchanged)
        val room = "testRoom"
        val firebaseUrl = "https://numbux-790d6-default-rtdb.europe-west1.firebasedatabase.app"
        val dbRef = Firebase.database(firebaseUrl)
            .getReference("rooms")
            .child(room)
            .child("blocking_enabled")

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remote = snapshot.getValue(Boolean::class.java) ?: false
                // write to prefs *and* state – triggers our prefListener
                prefs.edit().putBoolean("blocking_enabled", remote).apply()
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Firebase listen failed", error.toException())
            }
        })

        // 6) Now set up your Compose UI
        setContent {
            val enabled by blockingState
            NumbuxTheme {
                Scaffold(topBar = { TopAppBar(title = { Text("Numbux") }) }) { inner ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(inner)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Bienvenido a Numbux",
                            style = MaterialTheme.typography.headlineSmall)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bloqueo de apps")
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = enabled,
                                onCheckedChange = { isOn ->
                                    if (!isOn) {
                                        // ask PIN before disabling
                                        showDisablePinDialog { success ->
                                            if (success) {
                                                // both local & remote
                                                prefs.edit().putBoolean("blocking_enabled", false).apply()
                                                dbRef.setValue(false)
                                            }
                                        }
                                    } else {
                                        // enable immediately
                                        prefs.edit().putBoolean("blocking_enabled", true).apply()
                                        dbRef.setValue(true)
                                    }
                                }
                            )
                        }

                        Text(
                            if (enabled) "El bloqueador está ACTIVADO"
                            else "El bloqueador está DESACTIVADO",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isAccessibilityServiceEnabled(this)) {
            if (accessibilityDialog?.isShowing != true) {
                accessibilityDialog = AlertDialog.Builder(this)
                    .setTitle("Numbux sin Permisos")
                    .setMessage(
                        "Para que Numbux funcione correctamente, ve a:\n" +
                                "\n1. Accesibilidad → Apps Instaladas\n" +
                                "\n2. Numbux → ON → Aceptar"
                    )
                    .setCancelable(false)
                    .setPositiveButton("Abrir Ajustes") { _, _ ->
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .show()
            }
        } else {
            accessibilityDialog?.dismiss()
            accessibilityDialog = null
        }
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val component = ComponentName(context, AppBlockerService::class.java)
        val enabledServices = am
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        return enabledServices.any {
            val info = it.resolveInfo.serviceInfo
            info.packageName == component.packageName &&
                    info.name == component.className
        }
    }


    private fun showEnableAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("NumbuX sin Permisos")
            .setMessage(
                "Para que NumbuX funcione correctamente, ve a:\n" +
                        "\n" +
                        "1. Accesibilidad → Apps Instaladas\n" +
                        "\n" +
                        "2. Numbux → ON → Aceptar"
            )
            .setCancelable(false)
            .setPositiveButton("Abrir Ajustes") { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .show()
    }

    private fun showDisablePinDialog(onResult: (Boolean) -> Unit) {
        val pinInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
            hint = "####"
        }
        AlertDialog.Builder(this)
            .setTitle("Ingrese PIN para desactivar")
            .setView(pinInput)
            .setPositiveButton("OK") { _, _ ->
                val entered = pinInput.text.toString()
                val correct = prefs.getString("pin_app_lock", "1234")
                if (entered == correct) onResult(true)
                else {
                    Toast.makeText(this, "PIN incorrecto", Toast.LENGTH_SHORT).show()
                    onResult(false)
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> onResult(false) }
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) recreate()
            else {
                Toast.makeText(this,
                    "Se necesita permiso para mostrar sobre otras apps", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
    }
}