package com.entropy.stage.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val StageColorScheme = darkColorScheme(
    primary = StagePalette.Violet,
    onPrimary = StagePalette.Ink,
    primaryContainer = Color(0xFF2A2148),
    onPrimaryContainer = StagePalette.TextPrimary,
    secondary = StagePalette.VioletBright,
    background = StagePalette.Ink,
    onBackground = StagePalette.TextPrimary,
    surface = StagePalette.Graphite,
    onSurface = StagePalette.TextPrimary,
    surfaceVariant = StagePalette.Elevated,
    onSurfaceVariant = StagePalette.TextSecondary,
    outline = StagePalette.Hairline,
)

@Composable
fun StageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StageColorScheme,
        typography = Typography(
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                lineHeight = 16.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.25).sp,
            ),
        ),
        content = content,
    )
}
