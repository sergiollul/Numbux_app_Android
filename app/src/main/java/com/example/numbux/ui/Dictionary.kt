// Replace `com.example.numbux.ui` with your actual package
package com.example.numbux.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
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
                        .forEach { rawLine ->
                            val t = rawLine.trim()
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

    /**
     * Searches the entire dictionary asset for any Latin word containing [query].
     * Returns a list of styled AnnotatedStrings (same styling rules as paging).
     */
    fun searchAll(context: Context, query: String): List<AnnotatedString> {
        val results = mutableListOf<AnnotatedString>()
        try {
            context.assets.open(FILENAME)
                .bufferedReader()
                .useLines { lines ->
                    lines.forEach { rawLine ->
                        val t = rawLine.trim()
                        if (t.isEmpty() || t.startsWith("#")) return@forEach

                        val parts = t.split(":", limit = 2)
                        if (parts.size == 2) {
                            val latinRaw = parts[0].trim()
                            val latin = latinRaw.lowercase()
                            val spanish = parts[1].trim()
                            if (latin.contains(query.lowercase())) {
                                // Apply same styling logic
                                val styled = buildAnnotatedString {
                                    if (latin.contains(". sin")) {
                                        append("$latin → $spanish")
                                    } else {
                                        pushStyle(codeSpanStyle)
                                        append(latin)
                                        pop()
                                        append(" → $spanish")
                                    }
                                }
                                results.add(styled)
                            }
                        }
                    }
                }
        } catch (e: IOException) {
            Log.e("Dictionary", "Search failed for \"$query\"", e)
        }
        return results
    }
}

@Composable
fun DictionaryBottomBar() {
    val context = LocalContext.current

    // State for the search query
    var query by remember { mutableStateOf("") }
    // State to hold search results when user types
    var searchResults by remember { mutableStateOf<List<AnnotatedString>>(emptyList()) }
    // Determine if we’re in “search mode”
    val isSearching = query.trim().isNotEmpty()

    // Initial load of first page, only when not searching
    LaunchedEffect(Unit) {
        Dictionary.reset()
        Dictionary.loadNextPage(context)
    }

    // Whenever query changes, perform a full-text search
    LaunchedEffect(query) {
        if (query.isBlank()) {
            // Clear search results when query is empty
            searchResults = emptyList()
        } else {
            searchResults = Dictionary.searchAll(context, query.trim())
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // If this bottom bar lives inside a Scaffold.bottomBar, you might want to limit
            // its total height. Otherwise, you can let it expand as needed.
            .fillMaxHeight(0.92f) // for example, use 80% of screen height; adjust as needed
    ) {
        // ────────────────
        // 1️⃣ Search Field (always at top)
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Buscar…") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // ────────────────
        // 2️⃣ Either Search Results or Paginated Dictionary, filling remaining space
        Spacer(modifier = Modifier.height(4.dp))

        if (isSearching) {
            if (searchResults.isEmpty()) {
                // Show “no results” message, pinned at top of the list–area
                Text(
                    text = "No se encontraron resultados para \"$query\"",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .weight(1f) // this pushes it to take up rest of space if needed
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // <— Let the list fill all available height
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchResults) { styledText ->
                        Text(styledText)
                    }
                }
            }
        } else {
            if (Dictionary.styledEntries.isEmpty()) {
                Text(
                    "Cargando diccionario…",
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                        .weight(1f) // still take up rest of space
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f) // <— Let the list fill all available height
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(Dictionary.styledEntries) { index, styledText ->
                        Text(styledText)

                        // When reaching the last visible item, load the next page.
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
}

