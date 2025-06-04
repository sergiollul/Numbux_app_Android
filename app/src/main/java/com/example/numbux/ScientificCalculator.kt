package com.example.numbux.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ScientificCalculator() {
    // -------------- ESTADOS --------------
    var fieldValue by remember { mutableStateOf(TextFieldValue(text = "", selection = TextRange(0))) }
    var result     by remember { mutableStateOf("") }
    var showPiMenu by remember { mutableStateOf(false) }

    // Para gestionar el cursor en el BasicTextField
    val textFieldInteraction = remember { MutableInteractionSource() }
    val focusRequester       = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // Pequeño retraso para que Compose termine de “layoutear” y podamos solicitar foco
        delay(100)
        focusRequester.requestFocus()
        textFieldInteraction.tryEmit(FocusInteraction.Focus())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // ─── 1) BASIC TEXT FIELD ───────────────────────────────────────────
        BasicTextField(
            value             = fieldValue,
            onValueChange     = { fieldValue = it },
            readOnly          = true,
            interactionSource = textFieldInteraction,
            cursorBrush       = SolidColor(Color(0xFFFF6300)),
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .focusable(interactionSource = textFieldInteraction)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                // Alineamos el texto y el cursor a la derecha
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    inner()
                }
            }
        )

        // ─── 2) BOTÓN BACKSPACE ────────────────────────────────────────────
        Row(
            modifier             = Modifier
                .fillMaxWidth()
                .padding(end = 20.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = {
                    val pos = fieldValue.selection.start
                    if (pos > 0) {
                        val newText = fieldValue.text.removeRange(pos - 1, pos)
                        fieldValue = TextFieldValue(newText, TextRange(pos - 1))
                    }
                },
                modifier = Modifier.size(50.dp),
                colors   = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor   = Color(0xFFFF6300)
                )
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Backspace,
                    contentDescription = "Backspace",
                    modifier           = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Divider(
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            thickness = 2.dp,
            modifier  = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(30.dp))

        // ─── 3) GRID DE BOTONES ────────────────────────────────────────────
        Column(
            modifier            = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val buttons = listOf(
                listOf("C", "( )", "%",  "π-e"),
                listOf("xʸ","√", "log", "÷"),
                listOf("7",  "8", "9",  "×"),
                listOf("4",  "5", "6",  "−"),
                listOf("1",  "2", "3",  "+"),
                listOf("+/-","0", ".",  "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier             = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    row.forEach { label ->
                        // Para el efecto de “botón presionado”
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scaleFactor by animateFloatAsState(if (isPressed) 0.6f else 1f)

                        if (label == "π-e") {
                            // ─────────────────────────── BOTÓN π-e + DROPDOWN ───────────────────────────
                            Box(
                                modifier = Modifier
                                    .weight(1f)         // sigue en RowScope
                                    .aspectRatio(1f)
                                    .padding(3.dp)
                            ) {
                                Button(
                                    onClick            = { showPiMenu = true },
                                    interactionSource  = interactionSource,
                                    modifier           = Modifier.fillMaxSize(),
                                    colors             = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor   = LocalContentColor.current
                                    )
                                ) {
                                    Text(
                                        text         = "π-e",
                                        fontSize     = 34.sp,
                                        fontWeight   = FontWeight.Medium,
                                        fontFamily   = MaterialTheme.typography.titleLarge.fontFamily,
                                        modifier     = Modifier.scale(scaleFactor)
                                    )
                                }

                                // El “pequeño window” es un DropdownMenu que se cierra al tocar fuera
                                DropdownMenu(
                                    expanded         = showPiMenu,
                                    onDismissRequest = { showPiMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text    = { Text(text = "π") },
                                        onClick = {
                                            val sel = fieldValue.selection.start
                                            val newText =
                                                fieldValue.text.take(sel) + "π" + fieldValue.text.drop(sel)
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                            showPiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text    = { Text(text = "e") },
                                        onClick = {
                                            val sel = fieldValue.selection.start
                                            val newText =
                                                fieldValue.text.take(sel) + "e" + fieldValue.text.drop(sel)
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                            showPiMenu = false
                                        }
                                    )
                                }
                            }
                        } else {
                            // ───────────────────────────── BOTONES “NORMALES” ──────────────────────────
                            Button(
                                onClick = {
                                    when (label) {
                                        "C" -> {
                                            fieldValue = TextFieldValue("", TextRange(0))
                                        }
                                        "( )" -> {
                                            val text = fieldValue.text
                                            val sel  = fieldValue.selection.start
                                            val toInsert = if (!text.contains("(")) "(" else ")"
                                            val newText = buildString {
                                                append(text.take(sel))
                                                append(toInsert)
                                                append(text.drop(sel))
                                            }
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                        }
                                        "+/-" -> {
                                            val text = fieldValue.text
                                            val sel  = fieldValue.selection.start
                                            if (text.startsWith("-")) {
                                                val newText = text.removePrefix("-")
                                                val newSel  = (sel - 1).coerceAtLeast(0)
                                                fieldValue = TextFieldValue(newText, TextRange(newSel))
                                            } else {
                                                val newText = "-$text"
                                                fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                            }
                                        }
                                        "=" -> {
                                            val expr = fieldValue.text
                                                .replace("×", "*")
                                                .replace("÷", "/")
                                                .replace("−", "-")
                                            val res = evaluateExpression(expr)
                                            fieldValue = TextFieldValue(res, TextRange(res.length))
                                        }
                                        else -> {
                                            // Inserta dígito u operador en la posición actual del cursor
                                            if (result.isNotEmpty() && label.matches(Regex("[0-9.]"))) {
                                                // Si venimos de un resultado, empezamos un número nuevo
                                                fieldValue = TextFieldValue(label, TextRange(1))
                                                result     = ""
                                            } else {
                                                val sel = fieldValue.selection.start
                                                val newText = buildString {
                                                    append(fieldValue.text.take(sel))
                                                    append(label)
                                                    append(fieldValue.text.drop(sel))
                                                }
                                                fieldValue =
                                                    TextFieldValue(newText, TextRange(sel + label.length))
                                            }
                                        }
                                    }
                                },
                                interactionSource = interactionSource,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        label.matches(Regex("\\d")) || label in listOf(".", "+/-") ->
                                            Color(0xFF171719)
                                        label in listOf("÷", "×", "+", "−") ->
                                            Color(0xFFA6A6A6)
                                        label in listOf("C", "( )", "%") ->
                                            Color(0xFF2D2D2F)
                                        else ->
                                            MaterialTheme.colorScheme.primaryContainer
                                    },
                                    contentColor = when {
                                        label.matches(Regex("\\d")) || label in listOf(".", "+/-") ->
                                            Color.White
                                        label in listOf("C", "( )", "%") ->
                                            Color.White
                                        label in listOf("+", "−", "×", "÷") ->
                                            Color.Black
                                        else ->
                                            LocalContentColor.current
                                    }
                                )
                            ) {
                                Text(
                                    text = label,
                                    fontSize   = 34.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                                    modifier   = Modifier.scale(scaleFactor)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Evaluador de expresiones (Shunting‐Yard) ─────────────────────────────
private fun evaluateExpression(expr: String): String {
    return try {
        val rpn   = toRPN(expr)
        val value = evalRPN(rpn)
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    } catch (_: Exception) {
        "Error"
    }
}

private fun toRPN(expr: String): List<String> {
    val output = mutableListOf<String>()
    val ops    = ArrayDeque<Char>()
    var i      = 0
    while (i < expr.length) {
        when (val c = expr[i]) {
            in '0'..'9', '.' -> {
                val start = i
                while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) i++
                output += expr.substring(start, i)
                continue
            }
            '+', '-', '*', '/' -> {
                while (ops.isNotEmpty() && precedence(ops.first()) >= precedence(c)) {
                    output += ops.removeFirst().toString()
                }
                ops.addFirst(c)
            }
        }
        i++
    }
    while (ops.isNotEmpty()) output += ops.removeFirst().toString()
    return output
}

private fun evalRPN(tokens: List<String>): Double {
    val stack = ArrayDeque<Double>()
    for (tok in tokens) {
        when (tok) {
            "+" -> {
                val b = stack.removeFirst()
                val a = stack.removeFirst()
                stack.addFirst(a + b)
            }
            "-" -> {
                val b = stack.removeFirst()
                val a = stack.removeFirst()
                stack.addFirst(a - b)
            }
            "*" -> {
                val b = stack.removeFirst()
                val a = stack.removeFirst()
                stack.addFirst(a * b)
            }
            "/" -> {
                val b = stack.removeFirst()
                val a = stack.removeFirst()
                stack.addFirst(a / b)
            }
            else -> stack.addFirst(tok.toDouble())
        }
    }
    return stack.first()
}

private fun precedence(op: Char): Int = when (op) {
    '+', '-' -> 1
    '*', '/' -> 2
    else     -> 0
}
