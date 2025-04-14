package com.example.numbux

import android.content.Intent
import android.os.Bundle
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
import com.example.numbux.control.BlockManager
import com.example.numbux.ui.PinActivity
import com.example.numbux.ui.theme.NumbuxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bloquear YouTube como prueba
        BlockManager.blockApp("com.google.android.youtube")

        setContent {
            NumbuxTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
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
            // Lanzar actividad de PIN
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
