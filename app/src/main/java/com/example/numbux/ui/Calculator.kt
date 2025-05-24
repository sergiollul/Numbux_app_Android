package com.example.numbux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.focusable
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.background       // ← add this
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.foundation.interaction.FocusInteraction



@Composable
fun BasicCalculator() {

    var result     by remember { mutableStateOf("") }
    val textFieldInteraction = remember { MutableInteractionSource() }
    val focusRequester       = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        // give Compose a moment to lay out…
        delay(100)
        focusRequester.requestFocus()
        textFieldInteraction.tryEmit(FocusInteraction.Focus())
    }

    // We only need one state now: text + cursor position
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(0)))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 1) tappable, movable‐cursor display
        BasicTextField(
            value             = fieldValue,
            onValueChange     = { fieldValue = it },
            readOnly          = true,
            interactionSource = textFieldInteraction,
            cursorBrush       = SolidColor(Color(0xFFFF6300)),
            textStyle         = MaterialTheme.typography.headlineMedium.copy(
                color     = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .focusable(interactionSource = textFieldInteraction)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd     // ← push both text & caret to the right
                ) {
                    inner()
                }
            }
        )


        // ROW SOLO para el botón back, sin padding vertical extra
        Row(
            modifier = Modifier
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
                modifier = Modifier.size(50.dp),  // botón más grande…
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor   = Color(0xFFFF6300)
                )
            ) {
                Icon(
                    imageVector        = Icons.Outlined.Backspace,
                    contentDescription = "Backspace",
                    modifier           = Modifier.size(28.dp)  // …pero icono interno igual
                )
            }
        }

        // Separador con espacio arriba y abajo
        Spacer(modifier = Modifier.height(14.dp))
        Divider(
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            thickness = 2.dp,
            modifier  = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(30.dp))

        // Encapsulamos el grid de botones en su propia Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)   // sólo 4dp entre filas
        ) {

            // Layout de botones idéntico a tu captura:
            val buttons = listOf(
                listOf("C",  "( )", "%",  "÷"),
                listOf("7",  "8",  "9",  "×"),
                listOf("4",  "5",  "6",  "−"),
                listOf("1",  "2",  "3",  "+"),
                listOf("+/-","0",  ".",  "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        //.padding(vertical = 2.dp)    // espacio entre filas
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { label ->
                        // InteractionSource para animar al presionar
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scaleFactor by animateFloatAsState(if (isPressed) 0.6f else 1f)

                        Button(
                            onClick = {
                                when (label) {
                                    "C" -> {
                                        // Clear everything and reset caret
                                        fieldValue = TextFieldValue("", TextRange(0))
                                    }
                                    "( )" -> {
                                        // Toggle parentheses: if no “(” yet, insert “(” at cursor; else insert “)”
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
                                        // Toggle sign of the whole expression
                                        val text = fieldValue.text
                                        val sel  = fieldValue.selection.start
                                        if (text.startsWith("-")) {
                                            // remove leading “-”
                                            val newText = text.removePrefix("-")
                                            // move caret back by 1 (but not below 0)
                                            val newSel = (sel - 1).coerceAtLeast(0)
                                            fieldValue = TextFieldValue(newText, TextRange(newSel))
                                        } else {
                                            // add leading “-”
                                            val newText = "-$text"
                                            // move caret forward by 1
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
                                        // here’s the corrected nested if/else:
                                        if (result.isNotEmpty() && label.matches(Regex("[0-9.]"))) {
                                            // start a new number after a result
                                            fieldValue = TextFieldValue(label, TextRange(1))
                                            result     = ""
                                        } else {
                                            // insert at the current cursor position
                                            val sel     = fieldValue.selection.start
                                            val newText = buildString {
                                                append(fieldValue.text.take(sel))
                                                append(label)
                                                append(fieldValue.text.drop(sel))
                                            }
                                            fieldValue = TextFieldValue(newText, TextRange(sel + label.length))
                                        }
                                    }
                                }
                            },
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp),              // padding extra para que no se toquen
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
                            // Tamaño de fuente por etiqueta
                            val (fontSize, fontWeight) = when (label) {
                                "+/-" -> 16.sp to FontWeight.Light
                                "C", "( )" -> 26.sp to FontWeight.Light
                                "%" -> 28.sp to FontWeight.Light
                                "÷", "×", "+" -> 46.sp to FontWeight.Light
                                "−" -> 60.sp to FontWeight.Light
                                "=" -> 56.sp to FontWeight.Light
                                else -> 34.sp to FontWeight.Medium
                            }

                            Text(
                                text = label,
                                fontSize = fontSize,
                                fontWeight = fontWeight,
                                fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                                modifier = Modifier.scale(scaleFactor)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- Evaluador de expresiones simple (Shunting‐Yard) ---
private fun evaluateExpression(expr: String): String {
    return try {
        val rpn = toRPN(expr)
        val value = evalRPN(rpn)
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    } catch (_: Exception) {
        "Error"
    }
}

private fun toRPN(expr: String): List<String> {
    val output = mutableListOf<String>()
    val ops = ArrayDeque<Char>()
    var i = 0
    while (i < expr.length) {
        when (val c = expr[i]) {
            in '0'..'9', '.' -> {
                val start = i
                while (i < expr.length && (expr[i].isDigit() || expr[i]=='.')) i++
                output += expr.substring(start, i)
                continue
            }
            '+','-','*','/' -> {
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

private fun precedence(op: Char): Int = when(op) {
    '+','-' -> 1
    '*','/' -> 2
    else    -> 0
}