package com.example.numbux

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.example.numbux.ui.theme.NumbuxTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

@OptIn(ExperimentalMaterial3Api::class)
class ControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Firebase anonymous auth + database reference
        Firebase.auth.signInAnonymously()
        val room = "testRoom"
        val firebaseUrl =
            "https://numbux-790d6-default-rtdb.europe-west1.firebasedatabase.app"
        val dbRef = Firebase
            .database(firebaseUrl)
            .getReference("rooms")
            .child(room)
            .child("blocking_enabled")

        setContent {
            val context = LocalContext.current
            val prefs: SharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context)

            // Local UI state driven by remote DB
            var remoteEnabled by remember { mutableStateOf(false) }

            // Listen for remote changes and sync into SharedPreferences
            LaunchedEffect(dbRef) {
                dbRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val newVal = snapshot.getValue(Boolean::class.java) ?: false
                        remoteEnabled = newVal
                        // **Write into prefs so your service/UI everywhere picks it up**
                        prefs.edit()
                            .putBoolean("blocking_enabled", newVal)
                            .apply()
                    }
                    override fun onCancelled(error: DatabaseError) { /* log if desired */ }
                })
            }

            NumbuxTheme {
                Scaffold(
                    // make the scaffold itself transparent…
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentColor   = MaterialTheme.colorScheme.onBackground,
                    topBar         = { TopAppBar(title = { Text("Numbux Controller") }) }
                ) { padding ->
                    // 10 dummy switch states
                    val dummyStates = remember { List(10) { mutableStateOf(false) } }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Row with two master buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(onClick = {
                                // turn everything ON
                                remoteEnabled = true
                                prefs.edit().putBoolean("blocking_enabled", true).apply()
                                dbRef.setValue(true)
                                dummyStates.forEach { it.value = true }
                            }) {
                                Text("Turn All On")
                            }
                            Button(onClick = {
                                // turn everything OFF
                                remoteEnabled = false
                                prefs.edit().putBoolean("blocking_enabled", false).apply()
                                dbRef.setValue(false)
                                dummyStates.forEach { it.value = false }
                            }) {
                                Text("Turn All Off")
                            }
                        }

                        // Original “Modo Foco” switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Modo Foco")
                            Switch(
                                checked = remoteEnabled,
                                onCheckedChange = { newVal ->
                                    remoteEnabled = newVal
                                    prefs.edit()
                                        .putBoolean("blocking_enabled", newVal)
                                        .apply()
                                    dbRef.setValue(newVal)
                                }
                            )
                        }

                        // 10 dummy toggles
                        dummyStates.forEachIndexed { index, state ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dummy Toggle #${index + 1}")
                                Switch(
                                    checked = state.value,
                                    onCheckedChange = { state.value = it }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}