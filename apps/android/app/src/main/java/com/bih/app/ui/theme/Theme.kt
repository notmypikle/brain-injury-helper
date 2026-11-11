package com.bih.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

// GREEN ON PURPOSE (2026-11-11): green is the brain-injury awareness
// ribbon color. The user chose it the moment they learned that — "the
// green stays now for sure!" Deep greens keep text contrast strong
// (accessibility, risk O4); soft greens make calm highlights.
private val BihGreen = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF06210A),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEDC8),
    onSecondaryContainer = Color(0xFF0B260D),
    tertiary = Color(0xFF00695C),
    onTertiary = Color.White,
)

// Low cognitive load: big, calm text everywhere by default.
private val BihTypography = Typography(
    bodyLarge = Typography().bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge = Typography().titleLarge.copy(fontSize = 24.sp),
)

@Composable
fun BihTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BihGreen,
        typography = BihTypography,
        content = content,
    )
}
