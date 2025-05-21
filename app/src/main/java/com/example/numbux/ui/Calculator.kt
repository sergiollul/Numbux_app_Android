package com.example.numbux.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun BasicCalculator() {
    var expression by remember { mutableStateOf("") }
    var result     by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Pantalla de cálculo
        Text(
            text = if (result.isNotEmpty()) result else expression.ifEmpty { "0" },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.fillMaxWidth()
        )

        // Layout de botones idéntico a tu captura:
        val buttons = listOf(
            listOf("C",  "()", "%",  "÷"),
            listOf("7",  "8",  "9",  "×"),
            listOf("4",  "5",  "6",  "−"),
            listOf("1",  "2",  "3",  "+"),
            listOf("±",  "0",  ".",  "=")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { label ->
                    Button(
                        onClick = {
                            when (label) {
                                "C" -> {
                                    expression = ""
                                    result     = ""
                                }
                                "±" -> {
                                    expression = if (expression.startsWith("-"))
                                        expression.drop(1)
                                    else
                                        "-$expression"
                                }
                                "()" -> {
                                    // Inserta paréntesis balanceados
                                    expression += if (!expression.contains("(")) "(" else ")"
                                }
                                "=" -> {
                                    // Mapea símbolos a operadores estándar
                                    val toEval = expression
                                        .replace("×", "*")
                                        .replace("÷", "/")
                                        .replace("−", "-")
                                    result = evaluateExpression(toEval)
                                    expression = ""
                                }
                                else -> {
                                    // Números, decimales y operadores
                                    if (result.isNotEmpty() && label.matches(Regex("[0-9.]"))) {
                                        // si acabas de ver un resultado y escribes número, reinicia
                                        expression = label
                                        result     = ""
                                    } else {
                                        expression += label
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)      // todos los botones mismo peso
                            .aspectRatio(1f) // botones cuadrados
                    ) {
                        Text(label, style = MaterialTheme.typography.titleLarge)
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
