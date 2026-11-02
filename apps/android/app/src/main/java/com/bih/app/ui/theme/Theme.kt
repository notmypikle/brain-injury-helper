package com.bih.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// High-contrast, calm palette — chosen for readability and low cognitive load.
private val LightColors = lightColorScheme(
    primary = Color(0xFF2B5EA7),
    onPrimary = Color.White,
    secondary = Color(0xFF4A7C59),
    surface = Color(0xFFFAFAFA),
    background = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9EC3F5),
    onPrimary = Color(0xFF0B2447),
    secondary = Color(0xFFA8D5B5),
)

@Composable
fun BihTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        // Large default type scale helps users with cognitive/visual strain.
        typography = MaterialTheme.typography,
        content = content,
    )
}
