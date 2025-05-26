package com.example.numbux.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.Modifier


// Brand colors defined in Color.kt
//   val Orange6300: Color
//   val White: Color
//   val Black: Color
//   val ErrorLight: Color
//   val ErrorDark: Color

private val LightColorScheme = lightColorScheme(
    primary = Orange6300,
    onPrimary = White,
    primaryContainer = Orange6300,
    onPrimaryContainer = White,

    secondary = Orange6300,
    onSecondary = White,
    secondaryContainer = Orange6300,
    onSecondaryContainer = White,

    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,

    error = ErrorLight,
    onError = White
)

private val DarkColorScheme = darkColorScheme(
    primary = Orange6300,
    onPrimary = Black,
    primaryContainer = Orange6300,
    onPrimaryContainer = Black,

    secondary = Orange6300,
    onSecondary = Black,
    secondaryContainer = Orange6300,
    onSecondaryContainer = Black,

    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,

    error = ErrorDark,
    onError = Black
)

@Composable
fun NumbuxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography   = Typography
    ) {
        content()
    }
}

