package com.beam.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Seed: an electric cyan "beam of light" against a near-black surface.
private val LightColors = lightColorScheme(
    primary = Color(0xFF00696F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFA0EFF4),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF4A6365),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8EA),
    onSecondaryContainer = Color(0xFF051F21),
    tertiary = Color(0xFF4C5D7E),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD4E2FF),
    onTertiaryContainer = Color(0xFF041A38),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DD9E1),
    onPrimary = Color(0xFF00363A),
    primaryContainer = Color(0xFF004F55),
    onPrimaryContainer = Color(0xFFA0EFF4),
    secondary = Color(0xFFB1CBCD),
    onSecondary = Color(0xFF1B3436),
    secondaryContainer = Color(0xFF324B4D),
    onSecondaryContainer = Color(0xFFCCE8EA),
    tertiary = Color(0xFFB4C6EC),
    onTertiary = Color(0xFF1E2F4D),
    tertiaryContainer = Color(0xFF344764),
    onTertiaryContainer = Color(0xFFD4E2FF),
)

@Composable
fun BeamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
