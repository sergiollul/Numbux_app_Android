package com.example.numbux.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

// Brand colors
val Orange6300    = Color(0xFFFF6300)
val White         = Color(0xFFFFFFFF)
val Black         = Color(0xFF000000)
val ErrorLight    = Color(0xFFB00020)
val ErrorDark     = Color(0xFFCF6679)

// Light theme color scheme
val LightColors = lightColorScheme(
    primary               = Orange6300,
    onPrimary             = White,
    primaryContainer      = Orange6300,
    onPrimaryContainer    = White,

    secondary             = Orange6300,
    onSecondary           = White,
    secondaryContainer    = Orange6300,
    onSecondaryContainer  = White,

    background            = Black,
    onBackground          = White,
    surface               = Black,
    onSurface             = White,

    error                 = ErrorLight,
    onError               = White
)

// Dark theme color scheme
val DarkColors = darkColorScheme(
    primary               = Orange6300,
    onPrimary             = Black,
    primaryContainer      = Orange6300,
    onPrimaryContainer    = Black,

    secondary             = Orange6300,
    onSecondary           = Black,
    secondaryContainer    = Orange6300,
    onSecondaryContainer  = Black,

    background            = Black,
    onBackground          = White,
    surface               = Black,
    onSurface             = White,

    error                 = ErrorDark,
    onError               = Black
)
