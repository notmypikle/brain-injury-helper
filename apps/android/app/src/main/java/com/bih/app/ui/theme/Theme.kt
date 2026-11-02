package com.bih.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp

// Low cognitive load: big, calm text everywhere by default.
private val BihTypography = Typography(
    bodyLarge = Typography().bodyLarge.copy(fontSize = 20.sp, lineHeight = 28.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
    titleLarge = Typography().titleLarge.copy(fontSize = 24.sp),
)

@Composable
fun BihTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        typography = BihTypography,
        content = content,
    )
}
