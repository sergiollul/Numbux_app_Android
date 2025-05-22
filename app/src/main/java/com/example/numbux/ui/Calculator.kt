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



@Composable
fun BasicCalculator() {
    var expression by remember { mutableStateOf("") }
    var result     by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // Pantalla de cálculo
        Text(
            text = if (result.isNotEmpty()) result else expression.ifEmpty { "0" },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )

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
                                        expression = ""
                                        result     = ""
                                    }
                                    "+/-" -> {
                                        expression = if (expression.startsWith("-"))
                                            expression.drop(1)
                                        else
                                            "-$expression"
                                    }
                                    "( )" -> {
                                        expression += if (!expression.contains("(")) "(" else ")"
                                    }
                                    "=" -> {
                                        val toEval = expression
                                            .replace("×", "*")
                                            .replace("÷", "/")
                                            .replace("−", "-")
                                        result = evaluateExpression(toEval)
                                        expression = ""
                                    }
                                    else -> {
                                        if (result.isNotEmpty() && label.matches(Regex("[0-9.]"))) {
                                            expression = label
                                            result     = ""
                                        } else {
                                            expression += label
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