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



class MainActivity : ComponentActivity() {

    companion object {
        private const val REQ_OVERLAY = 1001
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expected = ComponentName(this, AppBlockerService::class.java)
            .flattenToString()
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return TextUtils.SimpleStringSplitter(':')
            .apply { setString(enabled) }
            .any { it.equals(expected, ignoreCase = true) }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Check overlay permission right away, *before* anything else
        if (!Settings.canDrawOverlays(this)) {
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")).also { intent ->
                // start with a request code so we can catch the result
                startActivityForResult(intent, REQ_OVERLAY)
            }
            // don’t finish() here—let onActivityResult fire when they're done
            return
        }

        // 2) Now you know you have the overlay permission,
        //    you can safely enable your blocker service, set up compose, etc.

        // 1) Overlay & Accessibility checks
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            finish()
            return
        }

        BlockManager.setBlockedAppsExcept(
            this,
            listOf(packageName, "com.android.settings", "com.android.systemui")
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

        // 2) Prefs & Firebase setup
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val activity = this

        Firebase.auth.signInAnonymously()

        val room = "testRoom"
        // 1) Point the SDK at your live DB
        val firebaseUrl = "https://numbux-790d6-default-rtdb.europe-west1.firebasedatabase.app"
        val database = Firebase.database(firebaseUrl)

        // 2) Reference your shared node
        val dbRef = database
            .getReference("rooms")
            .child(room)
            .child("blocking_enabled")


        // Host your Compose state here
        val blockingState = mutableStateOf(
            prefs.getBoolean("blocking_enabled", false)
        )

        // Listen for remote toggles
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remote = snapshot.getValue(Boolean::class.java) ?: false
                prefs.edit().putBoolean("blocking_enabled", remote).apply()
                blockingState.value = remote
            }
            override fun onCancelled(error: DatabaseError) {
                Log.w("MainActivity", "Firebase listen failed", error.toException())
            }
        })

        // 3) Compose UI
        setContent {
            // pull in our state
            val blockingEnabled by blockingState

            NumbuxTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Numbux") })
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Bienvenido a Numbux",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Bloqueo de apps")
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = blockingEnabled,
                                onCheckedChange = { enabled ->
                                    if (!enabled) {
                                        // PIN-entry dialog
                                        val pinInput = EditText(activity).apply {
                                            inputType =
                                                android.text.InputType
                                                    .TYPE_CLASS_NUMBER or
                                                        android.text.InputType
                                                            .TYPE_NUMBER_VARIATION_PASSWORD
                                            hint = "####"
                                        }
                                        AlertDialog.Builder(activity)
                                            .setTitle("Ingrese PIN para desactivar")
                                            .setView(pinInput)
                                            .setPositiveButton("OK") { _, _ ->
                                                val entered = pinInput.text.toString()
                                                val correct = prefs
                                                    .getString("pin_app_lock", "1234")
                                                if (entered == correct) {
                                                    // locally & remotely turn OFF
                                                    blockingState.value = false
                                                    prefs.edit()
                                                        .putBoolean("blocking_enabled", false)
                                                        .apply()
                                                    dbRef.setValue(false)
                                                } else {
                                                    Toast
                                                        .makeText(
                                                            activity,
                                                            "PIN incorrecto",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                }
                                            }
                                            .setNegativeButton("Cancelar", null)
                                            .show()
                                    } else {
                                        // turn ON immediately
                                        blockingState.value = true
                                        prefs.edit()
                                            .putBoolean("blocking_enabled", true)
                                            .apply()
                                        dbRef.setValue(true)
                                    }
                                }
                            )
                        }
                        Text(
                            if (blockingEnabled)
                                "El bloqueador está ACTIVADO"
                            else
                                "El bloqueador está DESACTIVADO",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_OVERLAY) {
            if (Settings.canDrawOverlays(this)) {
                // Great—permission granted. Restart onCreate logic:
                recreate()
            } else {
                Toast.makeText(this,
                    "Se necesita permiso para mostrar sobre otras apps",
                    Toast.LENGTH_LONG).show()
                finish()  // give up if they deny
            }
        }
    }

}