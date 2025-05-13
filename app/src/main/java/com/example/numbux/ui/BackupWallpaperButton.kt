package com.example.numbux.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext

@Composable
fun RestoreWallpaperButton(
    initialUri: Uri?,
    onUriPicked: (Uri) -> Unit
) {
    val context = LocalContext.current

    var backupWallpaperUri by rememberSaveable { mutableStateOf(initialUri) }

    val pickWallpaperLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.also {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                backupWallpaperUri = it
                onUriPicked(it)
            }
        }
    )

    Column {
        if (backupWallpaperUri == null) {
            Button(onClick = { pickWallpaperLauncher.launch(arrayOf("image/*")) }) {
                Text("Selecciona tu fondo de pantalla para restaurarlo después del bloqueo")
            }
        }
        // no else → nothing shows once you've picked
    }
}
