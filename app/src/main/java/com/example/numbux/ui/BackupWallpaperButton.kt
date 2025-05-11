package com.example.numbux.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BackupWallpaperButton(onUriChosen: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let(onUriChosen)
    }

    Button(onClick = {
        // user picks “their current” wallpaper image
        launcher.launch(arrayOf("image/*"))
    }) {
        Text("Selecciona tu fondo para restaurar")
    }
}
