// Replace `com.example.numbux.ui` with your actual package
package com.example.numbux.ui

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.IOException

object Dictionary {
    private const val FILENAME = "diccionario_latin_espanol.txt"
    private const val PAGE_SIZE = 200

    /** Backing list of (latin → spanish) pairs, observable by Compose */
    val entries = mutableStateListOf<Pair<String, String>>()

    /** How many pages we’ve already loaded */
    private var loadedPages = 0

    /** Clears and resets paging state */
    fun reset() {
        entries.clear()
        loadedPages = 0
    }

    /**
     * Loads the next “page” of [PAGE_SIZE] lines from assets,
     * parsing “latin: spanish” per line and appending into [entries].
     */
    fun loadNextPage(context: Context) {
        try {
            context.assets.open(FILENAME)
                .bufferedReader()
                .useLines { lines ->
                    val start = loadedPages * PAGE_SIZE
                    lines
                        .drop(start)
                        .take(PAGE_SIZE)
                        .forEach { line ->
                            val trimmed = line.trim()
                            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                            val parts = trimmed.split(":", limit = 2)
                            if (parts.size == 2) {
                                val latin = parts[0].trim().lowercase()
                                val spanish = parts[1].trim()
                                if (latin.isNotEmpty() && spanish.isNotEmpty()) {
                                    entries.add(latin to spanish)
                                }
                            }
                        }
                }
            loadedPages++
            Log.i("Dictionary", "Loaded page $loadedPages, total entries=${entries.size}")
        } catch (e: IOException) {
            Log.e("Dictionary", "Failed to load asset $FILENAME", e)
            if (entries.isEmpty()) {
                entries.add("error" to "¡No pude cargar el diccionario!")
            }
        }
    }
}

@Composable
fun DictionaryBottomBar() {
    val context = LocalContext.current

    // On first composition, reset and load the first page:
    LaunchedEffect(Unit) {
        Dictionary.reset()
        Dictionary.loadNextPage(context)
    }

    val entries = Dictionary.entries

    if (entries.isEmpty()) {
        Text("Cargando diccionario…")
    } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 760.dp)
                .padding(16.dp)
        ) {
            // state to observe scroll position
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(entries) { index, (latin, spanish) ->
                    Text("$latin → $spanish")

                    // When we've displayed the last item, load next page
                    if (index == entries.lastIndex) {
                        LaunchedEffect(entries.size) {
                            Dictionary.loadNextPage(context)
                        }
                    }
                }
            }
        }
    }
}
