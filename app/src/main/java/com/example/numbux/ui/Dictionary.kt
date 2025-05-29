// Replace `com.example.numbux.ui` with your actual package
package com.example.numbux.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException

/**
 * A simple dictionary mapping Latin words to their Spanish equivalents.
 * Entries are loaded at runtime from the TXT resource, up to [maxLines].
 */
object Dictionary {
    private val _latinToSpanish = mutableStateMapOf<String, String>()
    val latinToSpanish: Map<String, String> get() = _latinToSpanish

    /**
     * Load and parse at most [maxLines] lines from the asset file.
     * Pass maxLines = -1 to load the entire file.
     */
    fun loadFromAssets(
        context: Context,
        filename: String = "diccionario_latin_espanol.txt",
        maxLines: Int = 200
    ) {
        if (_latinToSpanish.isNotEmpty()) return

        try {
            context.assets.open(filename)
                .bufferedReader()
                .useLines { lines ->
                    // Cap the sequence if maxLines >= 0
                    val sequence = if (maxLines >= 0) lines.take(maxLines) else lines
                    sequence.forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

                        val parts = trimmed.split(":", limit = 2)
                        if (parts.size == 2) {
                            val latin = parts[0].trim().lowercase()
                            val spanish = parts[1].trim()
                            if (latin.isNotEmpty() && spanish.isNotEmpty()) {
                                _latinToSpanish[latin] = spanish
                            }
                        }
                    }
                }
            Log.i("Dictionary", "Loaded ${_latinToSpanish.size} entries (maxLines=$maxLines)")
        } catch (e: IOException) {
            Log.e("Dictionary", "Failed to load asset “$filename”", e)
            // Fallback so UI isn’t stuck empty:
            _latinToSpanish["error"] = "¡No pude cargar el diccionario!"
        }
    }
}

@Composable
fun DictionaryBottomBar() {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        // Load only the first 200 entries to keep startup light
        Dictionary.loadFromAssets(
            context,
            filename = "diccionario_latin_espanol.txt",
            maxLines = 200
        )
    }

    val entries = Dictionary.latinToSpanish.toList()

    if (entries.isEmpty()) {
        Text("Cargando diccionario…")
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            entries.forEach { (latin, spanish) ->
                Text("$latin → $spanish")
            }
        }
    }
}
