package com.entropy.stage.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object StagePalette {
    val Ink = Color(0xFF07080C)
    val Graphite = Color(0xFF101118)
    val Elevated = Color(0xFF181A24)
    val Hairline = Color(0xFF2A2D3B)
    val TextPrimary = Color(0xFFF4F2F8)
    val TextSecondary = Color(0xFFAAA7B6)
    val Violet = Color(0xFF9A7CFF)
    val VioletBright = Color(0xFFB9A7FF)
    val Success = Color(0xFF7DE2B8)
}

data class StageDimensions(
    val contentPadding: Dp,
    val topStripHeight: Dp,
    val dockHeight: Dp,
    val panelRadius: Dp,
    val gap: Dp,
    val minTouchTarget: Dp,
    val sidePanelWidth: Dp,
)

fun stageDimensionsFor(
    width: Dp,
    fontScale: Float,
): StageDimensions {
    val accessibleScale = fontScale.coerceIn(1f, 1.35f)
    val compact = width < 600.dp

    return StageDimensions(
        contentPadding = if (compact) 14.dp else 22.dp,
        topStripHeight = (if (compact) 48.dp else 52.dp) * accessibleScale,
        dockHeight = (if (compact) 62.dp else 68.dp) * accessibleScale,
        panelRadius = if (compact) 22.dp else 26.dp,
        gap = if (compact) 12.dp else 16.dp,
        minTouchTarget = 48.dp * accessibleScale,
        sidePanelWidth = if (compact) 0.dp else 296.dp,
    )
}
