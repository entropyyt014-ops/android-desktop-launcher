package com.entropy.stage.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
        typography = Typography(),
        content = content,
    )
}
