package com.example.numbux

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ktx.database
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.example.numbux.ui.theme.NumbuxTheme
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
class ControlActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Firebase anonymous auth + dbRef
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

        // 2) Compose UI
        setContent {
            var remoteEnabled by remember { mutableStateOf(false) }

            // listen for remote changes
            LaunchedEffect(Unit) {
                dbRef.addValueEventListener(object: ValueEventListener {
                    override fun onDataChange(snap: DataSnapshot) {
                        remoteEnabled = snap.getValue(Boolean::class.java) ?: false
                    }
                    override fun onCancelled(err: DatabaseError) { /* log if you want */ }
                })
            }

            NumbuxTheme {
                Scaffold(topBar = { TopAppBar(title = { Text("Numbux Controller") }) }) { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(p)
                            .padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Bloqueo de apps")
                        Switch(
                            checked = remoteEnabled,
                            onCheckedChange = { newVal ->
                                dbRef.setValue(newVal)
                            }
                        )
                    }
                }
            }
        }
    }
}
