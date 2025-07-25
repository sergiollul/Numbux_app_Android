package com.example.numbux.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import com.example.numbux.ui.BlinkingCursorField

@Composable
fun ScientificCalculator() {
    var fieldValue by remember { mutableStateOf(TextFieldValue(text = "", selection = TextRange(0))) }
    var showPiMenu by remember { mutableStateOf(false) }

    fun insertarOperador(op: String) {
        val sel = fieldValue.selection.start
        val newText = buildString {
            append(fieldValue.text.take(sel))
            append(op)
            append(fieldValue.text.drop(sel))
        }
        fieldValue = TextFieldValue(newText, TextRange(sel + op.length))
    }

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
        // ─── CUSTOM BLINKING CURSOR FIELD ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .focusable(interactionSource = textFieldInteraction)
                .focusRequester(focusRequester)
        ) {
            BlinkingCursorField(
                fieldValue    = fieldValue,
                onValueChange = { fieldValue = it },
                modifier      = Modifier.fillMaxSize()
            )
        }

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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFA6A6A6),
                                        contentColor   = Color.White
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
                        } else if (label == "( )") {
                            // ① Estado para detectar si ha ocurrido un long press
                            var isPressed by remember { mutableStateOf(false) }
                            var didLongPress by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp)
                                    // Forma circular + fondo
                                    .clip(CircleShape)
                                    .background(Color(0xFF2D2D2F))
                                    // ② PointerInput que gestiona tanto onPress como onLongPress
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                // Marcamos que estamos presionando
                                                isPressed = true
                                                try {
                                                    // Esperamos a que suelten el dedo (o cancelen)
                                                    awaitRelease()
                                                    // Solo si NO hubo longPress, hacemos el toggle de “(” / “)”
                                                    if (!didLongPress) {
                                                        val text = fieldValue.text
                                                        val sel = fieldValue.selection.start
                                                        val countOpen = text.count { it == '(' }
                                                        val countClose = text.count { it == ')' }
                                                        val toInsert =
                                                            if (countOpen > countClose) ")" else "("

                                                        val newText = buildString {
                                                            append(text.take(sel))
                                                            append(toInsert)
                                                            append(text.drop(sel))
                                                        }
                                                        fieldValue = TextFieldValue(
                                                            newText,
                                                            TextRange(sel + 1)
                                                        )
                                                    }
                                                } finally {
                                                    // Al terminar (up o cancel), reseteamos ambos flags
                                                    isPressed = false
                                                    didLongPress = false
                                                }
                                            },
                                            onLongPress = {
                                                // Cuando ocurre long press, insertamos siempre “(”
                                                didLongPress = true
                                                val text = fieldValue.text
                                                val sel = fieldValue.selection.start
                                                val newText = buildString {
                                                    append(text.take(sel))
                                                    append("(")
                                                    append(text.drop(sel))
                                                }
                                                fieldValue =
                                                    TextFieldValue(newText, TextRange(sel + 1))
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                // ③ Solo escalamos el texto según “isPressed”
                                Text(
                                    text = "( )",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White,
                                    modifier = Modifier.scale(if (isPressed) 0.85f else 1f)
                                )
                            }
                        }else if (label == "log") {
                            Button(
                                onClick = {
                                    val sel = fieldValue.selection.start
                                    val newText = buildString {
                                        append(fieldValue.text.take(sel))
                                        append("log(")
                                        append(fieldValue.text.drop(sel))
                                    }
                                    fieldValue = TextFieldValue(newText, TextRange(sel + 4))
                                },
                                interactionSource = interactionSource,
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(3.dp),
                                // Quitamos padding interno para dar espacio al texto grande:
                                contentPadding = PaddingValues(0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        label.matches(Regex("\\d")) || label == "." || label == "+/-" ->
                                            Color(0xFF171719)
                                        label in listOf("÷", "×", "+", "−", "xʸ", "√", "log") ->
                                            Color(0xFFA6A6A6)
                                        label in listOf("C", "( )", "%") ->
                                            Color(0xFF2D2D2F)
                                        else ->
                                            MaterialTheme.colorScheme.primaryContainer
                                    },
                                    contentColor = when {
                                        label.matches(Regex("\\d")) || label == "." || label == "+/-" ->
                                            Color.White
                                        label in listOf("C", "( )", "%") ->
                                            Color.White
                                        label in listOf("+", "−", "×", "÷", "xʸ", "√", "log") ->
                                            Color.Black
                                        else ->
                                            LocalContentColor.current
                                    }
                                )
                            ) {
                                BoxWithConstraints(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val heightDp = this.maxHeight
                                    val density  = LocalDensity.current

                                    // Ajusta el factor para que “log” quede del tamaño deseado:
                                    val fontSizeSp = with(density) {
                                        (heightDp * 0.38f).toSp() // por ejemplo 70% del alto
                                    }

                                    // ① Solo el Text lleva scaleFactor:
                                    Text(
                                        text       = "log",
                                        fontSize   = fontSizeSp,
                                        fontWeight = FontWeight.Normal,
                                        textAlign  = TextAlign.Center,
                                        maxLines   = 1,
                                        modifier   = Modifier.scale(scaleFactor)
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
                                        "+/-" -> {
                                            val text   = fieldValue.text
                                            val cursor = fieldValue.selection.start

                                            // 1) Encontrar el índice de inicio del número donde está el cursor:
                                            var startIndex = cursor
                                            while (startIndex > 0 && (text[startIndex - 1].isDigit() || text[startIndex - 1] == '.')) {
                                                startIndex--
                                            }

                                            // 2) Encontrar el índice de fin de ese mismo número:
                                            var endIndex = cursor
                                            while (endIndex < text.length && (text[endIndex].isDigit() || text[endIndex] == '.')) {
                                                endIndex++
                                            }

                                            // 3) Comprobar si justo antes de startIndex hay un signo “-” que actúe como unario:
                                            //    Debe ser un “-” y, o bien estar en la posición 0, o bien el carácter anterior
                                            //    a ese “-” es un operador o paréntesis izquierdo, para asegurarnos de que es un “-” unario.
                                            val hasMinus = startIndex > 0 && text[startIndex - 1] == '-'
                                            val isUnaryMinus = if (hasMinus) {
                                                // Si está en posición 1 (startIndex-1 == 0), es un “-” unario en el comienzo
                                                if (startIndex - 1 == 0) true
                                                else {
                                                    // Si el carácter anterior a ese “-” es uno de los operadores o “(”
                                                    val prev = text[startIndex - 2]
                                                    prev in listOf('+', '−', '×', '÷', '*', '/', '^', '(', '%')
                                                }
                                            } else {
                                                false
                                            }

                                            val newText: String
                                            val newCursor: Int

                                            if (isUnaryMinus) {
                                                // 4.a) Si ya había un “-” unario, lo quitamos:
                                                newText = text.removeRange(startIndex - 1, startIndex)
                                                newCursor = cursor - 1
                                            } else {
                                                // 4.b) Si no había un “-” unario, lo insertamos antes del número:
                                                newText = buildString {
                                                    append(text.take(startIndex))
                                                    append("-")
                                                    append(text.drop(startIndex))
                                                }
                                                newCursor = cursor + 1
                                            }

                                            fieldValue = TextFieldValue(newText, TextRange(newCursor))
                                        }
                                        "=" -> {
                                            val expr = fieldValue.text
                                            val res  = evaluateExpression(expr)
                                            fieldValue = TextFieldValue(res, TextRange(res.length))
                                        }
                                        in listOf("0","1","2","3","4","5","6","7","8","9",".") -> {
                                            val sel = fieldValue.selection.start
                                            val newText = buildString {
                                                append(fieldValue.text.take(sel))
                                                append(label)
                                                append(fieldValue.text.drop(sel))
                                            }
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                        }
                                        "+"  -> insertarOperador("+")
                                        "−"  -> insertarOperador("−")
                                        "×"  -> insertarOperador("×")
                                        "÷"  -> insertarOperador("÷")
                                        "%"  -> insertarOperador("%")
                                        "√" -> {
                                            val sel = fieldValue.selection.start
                                            val newText = buildString {
                                                append(fieldValue.text.take(sel))
                                                append("√(")
                                                append(fieldValue.text.drop(sel))
                                            }
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 2))
                                        }
                                        "xʸ" -> {
                                            val sel = fieldValue.selection.start
                                            val newText = buildString {
                                                append(fieldValue.text.take(sel))
                                                append("^")
                                                append(fieldValue.text.drop(sel))
                                            }
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                        }
                                        else -> {
                                            val sel = fieldValue.selection.start
                                            val newText = buildString {
                                                append(fieldValue.text.take(sel))
                                                append(label)
                                                append(fieldValue.text.drop(sel))
                                            }
                                            fieldValue = TextFieldValue(newText, TextRange(sel + label.length))
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
                                        label.matches(Regex("\\d")) || label == "." || label == "+/-" ->
                                            Color(0xFF171719)
                                        label in listOf("÷", "×", "+", "−", "xʸ", "√", "log") ->
                                            Color(0xFFA6A6A6)
                                        label in listOf("C", "( )", "%") ->
                                            Color(0xFF2D2D2F)
                                        else ->
                                            MaterialTheme.colorScheme.primaryContainer
                                    },
                                    contentColor = when {
                                        label.matches(Regex("\\d")) || label == "." || label == "+/-" ->
                                            Color.White
                                        label in listOf("C", "( )", "%") ->
                                            Color.White
                                        label in listOf("+", "−", "×", "÷", "xʸ", "√", "log") ->
                                            Color.Black
                                        else ->
                                            LocalContentColor.current
                                    }
                                )
                            ) {
                                val (fontSize, fontWeight) = when (label) {
                                    "+/-"        -> 16.sp to FontWeight.Light
                                    "C", "( )"   -> 26.sp to FontWeight.Light
                                    "%"          -> 28.sp to FontWeight.Light
                                    "÷", "×", "+", "√" -> 46.sp to FontWeight.Light
                                    "xʸ"         -> 36.sp to FontWeight.Light
                                    "−"          -> 60.sp to FontWeight.Light
                                    "="          -> 56.sp to FontWeight.Light
                                    else         -> 34.sp to FontWeight.Medium
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

// ───────────────────────────────────────────────────────────
// ─── A PARTIR DE AQUÍ VAN LAS FUNCIONES DE EVALUACIÓN ───
// ───────────────────────────────────────────────────────────

// 1) Para cerrar automáticamente paréntesis que falten
private fun balanceParentheses(input: String): String {
    var openCount = 0
    for (ch in input) {
        if (ch == '(') openCount++
        else if (ch == ')') openCount--
    }
    return if (openCount > 0) {
        input + ")".repeat(openCount)
    } else {
        input
    }
}

// 2) evaluateExpression usa balanceParentheses y luego toRPN + evalRPN
private fun evaluateExpression(exprInput: String): String {
    return try {
        var expr = exprInput
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("√", "sqrt")
            .replace("xʸ", "^")
            .replace("%", "/100")
            .trim()

        expr = balanceParentheses(expr)

        val rpnValue = toRPN(expr)
        val result   = evalRPN(rpnValue)

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

// 3) Convierte la expresión infija a una lista de tokens RPN
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

    fun isLeftAssociative(op: String): Boolean = (op != "^")

    var i = 0
    while (i < expr.length) {
        val c = expr[i]
        if (c.isWhitespace()) {
            i++
            continue
        }
        // Si es dígito o punto, parseamos el número completo
        if (c.isDigit() || c == '.') {
            val start = i
            while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                i++
            }
            output += expr.substring(start, i)
            continue
        }
        // Si es letra (función) o constante π/e
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
        // Paréntesis izquierdo
        if (c == '(') {
            ops.addFirst("(")
            i++
            continue
        }
        // Paréntesis derecho
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
            // Determinar si es “-” unario
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

// 4) Evalúa la lista de tokens RPN y devuelve un Double
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
                    "%" -> a * (b / 100.0)
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
