package com.example.numbux.ui

import android.app.WallpaperManager
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

@Composable
fun BlockerToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)  // 6.dp de separación
    ) {
        Text("Modo Foco:")
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked          = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    // Botón ON
                    checkedThumbColor  = Color(0xFF000000),
                    checkedBorderColor = Color(0xFF000000),
                    checkedTrackColor  = Color(0xFFFF6300),
                    // Botón OFF
                    uncheckedThumbColor  = Color(0xFFFF6300),
                    uncheckedBorderColor = Color(0xFFFF6300),
                    uncheckedTrackColor  = Color(0xFF000000)
                )
            )
            Text(
                text = if (enabled) "Activado" else "Desactivado",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White  // o el color que prefieras
            )
        }
    }
}
