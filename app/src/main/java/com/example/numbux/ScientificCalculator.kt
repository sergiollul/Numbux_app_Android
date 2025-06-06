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
import kotlin.math.*


@Composable
fun ScientificCalculator() {
    var fieldValue by remember { mutableStateOf(TextFieldValue(text = "", selection = TextRange(0))) }
    var showPiMenu by remember { mutableStateOf(false) }

    // Para gestionar el foco en el BasicTextField
    val textFieldInteraction = remember { MutableInteractionSource() }
    val focusRequester       = remember { FocusRequester() }

    LaunchedEffect(Unit) {
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
        // ─── BASIC TEXT FIELD ──────────────────────────────────────────────
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
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    inner()
                }
            }
        )

        // ─── BACKSPACE ─────────────────────────────────────────────────────
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

        // ─── GRID DE BOTONES ───────────────────────────────────────────────
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
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scaleFactor by animateFloatAsState(if (isPressed) 0.6f else 1f)

                        if (label == "π-e") {
                            // ─── BOTÓN π-e + DROPDOWN ───────────────────────────────
                            Box(
                                modifier = Modifier
                                    .weight(1f)
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
                                        text       = "π-e",
                                        fontSize   = 34.sp,
                                        fontWeight = FontWeight.Medium,
                                        fontFamily = MaterialTheme.typography.titleLarge.fontFamily,
                                        modifier   = Modifier.scale(scaleFactor)
                                    )
                                }

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
                                    DropdownMenuItem(
                                        text    = { Text(text = "ln") },
                                        onClick = {
                                            val sel = fieldValue.selection.start
                                            val newText =
                                                fieldValue.text.take(sel) + "ln(" + fieldValue.text.drop(sel)
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 3))
                                            showPiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text    = { Text(text = "sin") },
                                        onClick = {
                                            val sel = fieldValue.selection.start
                                            val newText =
                                                fieldValue.text.take(sel) + "sin(" + fieldValue.text.drop(sel)
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 4))
                                            showPiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text    = { Text(text = "cos") },
                                        onClick = {
                                            val sel = fieldValue.selection.start
                                            val newText =
                                                fieldValue.text.take(sel) + "cos(" + fieldValue.text.drop(sel)
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 4))
                                            showPiMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text    = { Text(text = "tan") },
                                        onClick = {
                                            val sel = fieldValue.selection.start
                                            val newText =
                                                fieldValue.text.take(sel) + "tan(" + fieldValue.text.drop(sel)
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 4))
                                            showPiMenu = false
                                        }
                                    )
                                }
                            }
                        } else {
                            // ─── BOTONES NORMALES ─────────────────────────────────
                            Button(
                                onClick = {
                                    when (label) {
                                        "C" -> {
                                            fieldValue = TextFieldValue("", TextRange(0))
                                        }
                                        "( )" -> {
                                            val text = fieldValue.text
                                            val sel  = fieldValue.selection.start
                                            val toInsert = "("  // simplemente inserta “(”
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
                                            // Reemplaza “×”/“÷”/“−”/“xʸ” en evaluateExpression
                                            val expr = fieldValue.text
                                            val res = evaluateExpression(expr)
                                            fieldValue = TextFieldValue(res, TextRange(res.length))
                                        }
                                        else -> {
                                            // Inserta dígito u operador (cubre “%”, “÷”, “×”, “−”, “+”, “.”, dígitos)
                                            if (label == "xʸ") {
                                                // inserta “^” en vez de la flecha
                                                val sel = fieldValue.selection.start
                                                val newText = buildString {
                                                    append(fieldValue.text.take(sel))
                                                    append("^")
                                                    append(fieldValue.text.drop(sel))
                                                }
                                                fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                            } else if (label == "√") {
                                                // inserta “√(” para que el parser lo convierta a “sqrt(”
                                                val sel = fieldValue.selection.start
                                                val newText = buildString {
                                                    append(fieldValue.text.take(sel))
                                                    append("√(")
                                                    append(fieldValue.text.drop(sel))
                                                }
                                                fieldValue = TextFieldValue(newText, TextRange(sel + 2))
                                            } else {
                                                // caso normal: números y {%, ÷, ×, −, +, .}
                                                val sel = fieldValue.selection.start
                                                val newText = buildString {
                                                    append(fieldValue.text.take(sel))
                                                    append(label)
                                                    append(fieldValue.text.drop(sel))
                                                }
                                                fieldValue = TextFieldValue(
                                                    newText,
                                                    TextRange(sel + label.length)
                                                )
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
                                    text       = label,
                                    fontSize   = fontSize,
                                    fontWeight = fontWeight,
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


// ─── Evaluador de expresiones (Shunting‐Yard completo) ─────────────────────

// 1) Función que cierra todos los paréntesis abiertos
private fun balanceParentheses(input: String): String {
    var openCount = 0
    for (ch in input) {
        if (ch == '(') openCount++
        else if (ch == ')') openCount--
    }
    // Si openCount > 0, faltan tantos ‘)’ al final
    return if (openCount > 0) {
        input + ")".repeat(openCount)
    } else {
        input
    }
}

// 2) evaluateExpression modificado para “auto‐cerrar” paréntesis
private fun evaluateExpression(exprInput: String): String {
    return try {
        // 2.a Normalizas símbolos especiales (√ → sqrt, × → *, ÷ → /, − → - …)
        var expr = exprInput
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("√", "sqrt")
            .replace("xʸ", "^")
            .replace("%", "/100")
            .trim()

        // 2.b Balanceas paréntesis: si falta un “)” al final, se agrega aquí
        expr = balanceParentheses(expr)

        // 2.c Llamas al resto de la lógica de RPN
        val rpnValue = toRPN(expr)
        val result   = evalRPN(rpnValue)

        // 2.d Formateas el resultado (sin “.0” si es entero)
        if (result.isInfinite() || result.isNaN()) {
            "Error"
        } else {
            if (result % 1.0 == 0.0) {
                result.toLong().toString()
            } else {
                result.toString().trimEnd('0').trimEnd('.')
            }
        }
    } catch (_: Exception) {
        "Error"
    }
}


// 3) toRPN: convierte la cadena infija en tokens RPN
private fun toRPN(expr: String): List<String> {
    val output = mutableListOf<String>()
    val ops = ArrayDeque<String>()

    fun precedence(op: String): Int = when (op) {
        "sin", "cos", "tan", "log", "ln", "sqrt" -> 5
        "^"   -> 4
        "*", "/", "%" -> 3
        "+", "-" -> 2
        else    -> 0
    }

    fun isLeftAssociative(op: String): Boolean = when (op) {
        "^" -> false
        else -> true
    }

    var i = 0
    while (i < expr.length) {
        val c = expr[i]

        if (c.isWhitespace()) {
            i++
            continue
        }

        // Números
        if (c.isDigit() || c == '.') {
            val start = i
            while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                i++
            }
            output += expr.substring(start, i)
            continue
        }

        // Funciones o constantes
        if (c.isLetter() || c == 'π' || c == 'e') {
            val start = i
            while (i < expr.length && (expr[i].isLetter() || expr[i] == 'π' || expr[i] == 'e')) {
                i++
            }
            val token = expr.substring(start, i)
            when (token) {
                "π", "e" -> output += token
                "sqrt", "sin", "cos", "tan", "log", "ln" -> ops.addFirst(token)
                else -> throw IllegalArgumentException("Unknown token: $token")
            }
            continue
        }

        // Paréntesis abriendo
        if (c == '(') {
            ops.addFirst("(")
            i++
            continue
        }

        // Paréntesis cerrando
        if (c == ')') {
            while (ops.isNotEmpty() && ops.first() != "(") {
                output += ops.removeFirst()
            }
            if (ops.isNotEmpty() && ops.first() == "(") {
                ops.removeFirst()
            }
            if (ops.isNotEmpty() && ops.first() in setOf("sin", "cos", "tan", "log", "ln", "sqrt")) {
                output += ops.removeFirst()
            }
            i++
            continue
        }

        // Operadores: +, -, *, /, ^, %
        if (c in listOf('+', '-', '*', '/', '^', '%')) {
            val op = c.toString()

            // Determinar “-” unario
            val isUnaryMinus = (c == '-') && (i == 0 ||
                    expr[i - 1] == '(' ||
                    expr[i - 1] in listOf('+', '-', '*', '/', '^'))

            if (isUnaryMinus) {
                while (ops.isNotEmpty() && precedence(ops.first()) >= precedence("u-")) {
                    output += ops.removeFirst()
                }
                ops.addFirst("u-")
                i++
                continue
            }

            // Operador binario normal
            while (ops.isNotEmpty()) {
                val top = ops.first()
                if ((top in setOf("+", "-", "*", "/", "^", "%")) &&
                    ( precedence(top) > precedence(op) ||
                            (precedence(top) == precedence(op) && isLeftAssociative(op))
                            )
                ) {
                    output += ops.removeFirst()
                    continue
                }
                break
            }
            ops.addFirst(op)
            i++
            continue
        }

        throw IllegalArgumentException("Invalid character at $i: '$c'")
    }

    while (ops.isNotEmpty()) {
        val x = ops.removeFirst()
        if (x == "(" || x == ")") {
            throw IllegalArgumentException("Mismatched parentheses")
        }
        output += x
    }

    return output
}

// 4) evalRPN: recorre los tokens RPN y calcula el valor final
private fun evalRPN(tokens: List<String>): Double {
    val stack = ArrayDeque<Double>()

    for (tok in tokens) {
        when {
            tok == "π" -> stack.addFirst(Math.PI)
            tok == "e" -> stack.addFirst(Math.E)
            tok == "u-" -> {
                val x = stack.removeFirst()
                stack.addFirst(-x)
            }
            tok in setOf("+", "-", "*", "/", "^", "%") -> {
                require(stack.size >= 2) { "Not enough operands for $tok" }
                val b = stack.removeFirst()
                val a = stack.removeFirst()
                val res = when (tok) {
                    "+" -> a + b
                    "-" -> a - b
                    "*" -> a * b
                    "/" -> a / b
                    "^" -> a.pow(b)
                    "%" -> a * (b / 100.0) // como ejemplo “a % b” → a * (b/100)
                    else -> throw IllegalStateException("Unknown operator $tok")
                }
                stack.addFirst(res)
            }
            tok in setOf("sin", "cos", "tan", "log", "ln", "sqrt") -> {
                require(stack.isNotEmpty()) { "No argument for function $tok" }
                val x = stack.removeFirst()
                val res = when (tok) {
                    "sin"  -> sin(x)
                    "cos"  -> cos(x)
                    "tan"  -> tan(x)
                    "log"  -> log10(x)
                    "ln"   -> ln(x)
                    "sqrt" -> sqrt(x)
                    else   -> throw IllegalStateException("Unknown function $tok")
                }
                stack.addFirst(res)
            }
            else -> {
                // token es un literal numérico
                val num = tok.toDoubleOrNull()
                    ?: throw IllegalArgumentException("Invalid token: $tok")
                stack.addFirst(num)
            }
        }
    }

    if (stack.size != 1) {
        throw IllegalArgumentException("Invalid expression: stack = $stack")
    }

    return stack.first()
}