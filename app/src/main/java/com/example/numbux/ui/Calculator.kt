package com.example.numbux.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import kotlinx.coroutines.delay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BlinkingCursorField(
    fieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var showCursor by remember { mutableStateOf(true) }
    val density = LocalDensity.current

    // Parpadeo
    LaunchedEffect(fieldValue.selection) {
        while (true) {
            delay(450)
            showCursor = !showCursor
        }
    }

    BasicTextField(
        value = fieldValue,
        onValueChange = onValueChange,
        readOnly = true,
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = MaterialTheme.typography.headlineMedium.copy(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End
        ),
        interactionSource = remember { MutableInteractionSource() },
        onTextLayout = { textLayout = it },
        modifier = modifier
    ) { inner ->
        Box(Modifier.fillMaxSize()) {
            // ⇒ cambiar a TOP_END para multiline
            Box(Modifier.align(Alignment.TopEnd)) {
                inner()

                if (showCursor && textLayout != null) {
                    val layout = textLayout!!

                    // 1) clamp selection to [0..textLen]
                    val textLen = fieldValue.text.length
                    val sel = fieldValue.selection.start.coerceIn(0, textLen)

                    // 2) try to get a CursorRect – if that fails, bail out early
                    val cursorRect = runCatching { layout.getCursorRect(sel) }
                        .getOrNull()
                        ?: return@Box  // skip drawing this frame

                    // 3) now draw the caret exactly where getCursorRect told us to
                    Box(
                        Modifier
                            .offset {
                                IntOffset(
                                    x = cursorRect.left.roundToInt(),
                                    y = cursorRect.top.roundToInt()
                                )
                            }
                            .width(2.dp)
                            .height(with(density) { cursorRect.height.toDp() })
                            .background(Color(0xFFFF6300))
                    )
                }
            }
        }
    }
}

@Composable
fun BasicCalculator() {
    var result by remember { mutableStateOf("") }
    val textFieldInteraction = remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }

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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .focusRequester(focusRequester)
                .focusable(interactionSource = textFieldInteraction)
        ) {
            BlinkingCursorField(
                fieldValue = fieldValue,
                onValueChange = { fieldValue = it },
                modifier = Modifier.fillMaxSize()
            )
        }

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
                modifier = Modifier.size(50.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color(0xFFFF6300)
                )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Backspace,
                    contentDescription = "Backspace",
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Separador con espacio arriba y abajo
        Spacer(modifier = Modifier.height(14.dp))
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
            thickness = 2.dp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(30.dp))

        // Encapsulamos el grid de botones en su propia Column
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Layout de botones idéntico a tu captura:
            val buttons = listOf(
                listOf("C", "( )", "%", "÷"),
                listOf("7", "8", "9", "×"),
                listOf("4", "5", "6", "−"),
                listOf("1", "2", "3", "+"),
                listOf("+/-", "0", ".", "=")
            )

            buttons.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { label ->
                        // InteractionSource para animar al presionar
                        val interactionSource = remember { MutableInteractionSource() }
                        val isPressed by interactionSource.collectIsPressedAsState()
                        val scaleFactor by animateFloatAsState(if (isPressed) 0.6f else 1f)

                        if (label == "( )") {
                            // ① Estado para detectar si ha ocurrido un long press
                            var isPressedParent by remember { mutableStateOf(false) }
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
                                                isPressedParent = true
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
                                                    isPressedParent = false
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
                                // ③ Solo escalamos el texto según “isPressedParent”
                                Text(
                                    text = "( )",
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Light,
                                    color = Color.White,
                                    modifier = Modifier.scale(if (isPressedParent) 0.85f else 1f)
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    when (label) {
                                        "C" -> {
                                            // Clear everything and reset caret
                                            fieldValue = TextFieldValue("", TextRange(0))
                                        }
                                        "%" -> {
                                            val text = fieldValue.text
                                            val sel = fieldValue.selection.start
                                            val newText = buildString {
                                                append(text.take(sel))
                                                append("%")
                                                append(text.drop(sel))
                                            }
                                            fieldValue = TextFieldValue(newText, TextRange(sel + 1))
                                        }
                                        "+/-" -> {
                                            val text = fieldValue.text
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
                                            val hasMinus = startIndex > 0 && text[startIndex - 1] == '-'
                                            val isUnaryMinus = if (hasMinus) {
                                                if (startIndex - 1 == 0) true
                                                else {
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
                                                .replace("×", "*")
                                                .replace("÷", "/")
                                                .replace("−", "-")
                                            val res = evaluateExpression(expr)
                                            fieldValue = TextFieldValue(res, TextRange(res.length))
                                        }
                                        else -> {
                                            if (result.isNotEmpty() && label.matches(Regex("[0-9.]"))) {
                                                // start a new number after a result
                                                fieldValue = TextFieldValue(label, TextRange(1))
                                                result = ""
                                            } else {
                                                val sel = fieldValue.selection.start
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
                                    .padding(3.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when {
                                        label.matches(Regex("\\d")) || label in listOf(".", "+/-") ->
                                            Color(0xFF171719)
                                        label in listOf("÷", "×", "+", "−") ->
                                            Color(0xFFA6A6A6)
                                        label in listOf("C", "%") ->
                                            Color(0xFF2D2D2F)
                                        else ->
                                            MaterialTheme.colorScheme.primaryContainer
                                    },
                                    contentColor = when {
                                        label.matches(Regex("\\d")) || label in listOf(".", "+/-") ->
                                            Color.White
                                        label in listOf("C", "%") ->
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
                                    "C" -> 26.sp to FontWeight.Light
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
    else -> 0
}
