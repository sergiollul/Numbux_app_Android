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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.io.IOException

object Dictionary {
    private const val FILENAME = "diccionario_latin_espanol.txt"
    private const val PAGE_SIZE = 200

    // raw data pairs
    private val _pairs = mutableListOf<Pair<String, String>>()
    // already‐styled strings ready for rendering
    val styledEntries = mutableStateListOf<AnnotatedString>()
    private var loadedPages = 0

    fun reset() {
        _pairs.clear()
        styledEntries.clear()
        loadedPages = 0
    }

    fun loadNextPage(context: Context) {
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
                                val latin  = parts[0].trim().lowercase()
                                val spanish = parts[1].trim()
                                if (latin.isNotEmpty() && spanish.isNotEmpty()) {
                                    // 1) store the raw pair (if you need it elsewhere)
                                    _pairs.add(latin to spanish)
                                    // 2) build a single AnnotatedString with the pre→ part styled
                                    val styled = buildAnnotatedString {
                                        pushStyle(
                                            SpanStyle(
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6200EE)
                                            )
                                        )
                                        append(latin)    // styled prefix
                                        pop()
                                        append(" → $spanish")  // normal suffix
                                    }
                                    styledEntries.add(styled)
                                }
                            }
                        }
                }
            loadedPages++
            Log.i("Dictionary", "Loaded page $loadedPages (total items=${styledEntries.size})")
        } catch (e: IOException) {
            Log.e("Dictionary", "Failed loading $FILENAME", e)
            if (styledEntries.isEmpty()) {
                styledEntries.add(
                    AnnotatedString("¡No pude cargar el diccionario!")
                )
            }
        }
    }
}

@Composable
fun DictionaryBottomBar() {
    val context = LocalContext.current

    // on first show, reset & load page 1
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
