package com.example.numbux.ui

import android.app.WallpaperManager
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text          // for text labels
import androidx.compose.ui.unit.dp            // for Spacer(width = ...)
import androidx.compose.ui.Alignment         // for Row(verticalAlignment = …)


@Composable
fun MySettingsScreen(
    enabled: Boolean,
    onToggle: (Boolean, WallpaperManager) -> Unit
) {
    // This is your Compose scope, so it's OK to call LocalContext.current here:
    val context = LocalContext.current
    val wallpaperManager = WallpaperManager.getInstance(context)

    Switch(
        checked = enabled,
        onCheckedChange = { isOn ->
            onToggle(isOn, wallpaperManager)
        }
    )
}
