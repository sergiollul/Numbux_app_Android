package com.example.numbux.ui

import android.app.WallpaperManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment

@Composable
fun BlockerToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Bloqueo de apps")
        Spacer(Modifier.width(8.dp))
        Switch(
            checked          = enabled,
            onCheckedChange = onToggle
        )
    }
}
