// Replace `com.example.numbux.ui` with your actual package
package com.example.numbux.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.IOException

object Dictionary {
    private const val FILENAME = "diccionario_latin_espanol.txt"
    private const val PAGE_SIZE = 200
    private const val MAX_PAGES_IN_RAM = 10
    private val MAX_ENTRIES_IN_RAM = MAX_PAGES_IN_RAM * PAGE_SIZE

    // raw data pairs (if you need them elsewhere)
    private val _pairs = mutableListOf<Pair<String, String>>()
    // already‐styled strings ready for rendering
    val styledEntries = mutableStateListOf<AnnotatedString>()
    private var loadedPages = 0
    private var isLoading = false

    fun reset() {
        _pairs.clear()
        styledEntries.clear()
        loadedPages = 0
        isLoading = false
    }

    private val codeSpanStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        color = Color.White,
        background = Color(0xFF616161)
    )

    /**
     * Loads the next page of entries (PAGE_SIZE lines), styling only those
     * Latin words that do not contain ". sin" with a code‐style span.
     * Evicts oldest entries when more than MAX_ENTRIES_IN_RAM are loaded.
     */
    fun loadNextPage(context: Context) {
        // Prevent multiple simultaneous loads
        if (isLoading) return
        isLoading = true

        try {
            context.assets.open(FILENAME)
                .bufferedReader()
                .useLines { lines ->
                    val start = loadedPages * PAGE_SIZE
                    lines.drop(start)
                        .take(PAGE_SIZE)
                        .forEach { line ->
                            val t = line.trim()
                            if (t.isEmpty() || t.startsWith("#")) return@forEach

                            val parts = t.split(":", limit = 2)
                            if (parts.size == 2) {
                                val latinRaw = parts[0].trim()
                                val latin = latinRaw.lowercase()
                                val spanish = parts[1].trim()
                                if (latin.isNotEmpty() && spanish.isNotEmpty()) {
                                    _pairs.add(latin to spanish)

                                    val styled = buildAnnotatedString {
                                        if (latin.contains(". sin")) {
                                            // no code styling for this entry
                                            append("$latin → $spanish")
                                        } else {
                                            // apply code‐style to the Latin word
                                            pushStyle(codeSpanStyle)
                                            append(latin)
                                            pop()
                                            // then arrow + Spanish normally
                                            append(" → $spanish")
                                        }
                                    }
                                    styledEntries.add(styled)
                                }
                            }
                        }
                }

            loadedPages++
            Log.i("Dictionary", "Loaded page $loadedPages (total items=${styledEntries.size})")

            // Evict oldest entries if we exceed MAX_ENTRIES_IN_RAM
            if (styledEntries.size > MAX_ENTRIES_IN_RAM) {
                val overflow = styledEntries.size - MAX_ENTRIES_IN_RAM
                repeat(overflow) {
                    if (styledEntries.isNotEmpty()) {
                        styledEntries.removeAt(0)
                    }
                }
                // If desired, also evict from raw _pairs:
                // repeat(overflow) { if (_pairs.isNotEmpty()) _pairs.removeAt(0) }

                Log.i(
                    "Dictionary",
                    "Evicted $overflow oldest entries; now ${styledEntries.size} remain in RAM"
                )
            }
        } catch (e: IOException) {
            Log.e("Dictionary", "Failed loading $FILENAME", e)
            if (styledEntries.isEmpty()) {
                styledEntries.add(AnnotatedString("¡No pude cargar el diccionario!"))
            }
        } finally {
            isLoading = false
        }
    }
}

@Composable
fun DictionaryBottomBar() {
    val context = LocalContext.current

    // on first show, reset & load first page
    LaunchedEffect(Unit) {
        Dictionary.reset()
        Dictionary.loadNextPage(context)
    }

    if (Dictionary.styledEntries.isEmpty()) {
        Text("Cargando diccionario…", modifier = Modifier.padding(16.dp))
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .padding(16.dp)
        ) {
            val state = rememberLazyListState()
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(Dictionary.styledEntries) { index, styledText ->
                    Text(styledText)
                    // when we hit the bottom, load the next page
                    if (index == Dictionary.styledEntries.lastIndex) {
                        LaunchedEffect(Dictionary.styledEntries.size) {
                            Dictionary.loadNextPage(context)
                        }
                    }
                }
            }
        }
    }
}
