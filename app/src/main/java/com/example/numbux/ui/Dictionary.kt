// Replace `com.example.numbux.ui` with your actual package
package com.example.numbux.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A simple dictionary mapping Latin words to their Spanish equivalents.
 * You can extend this map with more entries as needed.
 */
object Dictionary {
    /**
     * Map of Latin words (lowercase) to their Spanish translations.
     */
    val latinToSpanish: Map<String, String> = mapOf(
        "salve" to "hola",
        "amicus" to "amigo",
        "pax" to "paz",
        "aqua" to "agua",
        "terra" to "tierra",
        "sol" to "sol",
        "luna" to "luna"
        // add more entries here
    )

    /**
     * Translate a Latin word into Spanish. Returns null if not found.
     */
    fun translate(latin: String): String? {
        return latinToSpanish[latin.lowercase()]
    }
}

/**
 * A Composable that displays all entries in the Latin→Spanish dictionary.
 */
@Composable
fun DictionaryBottomBar() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Dictionary.latinToSpanish.forEach { (latin, spanish) ->
            Text(text = "$latin → $spanish")
        }
    }
}
