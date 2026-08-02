package com.entropy.stage.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object StagePalette {
    val Ink = Color(0xFF08090D)
    val Graphite = Color(0xFF111219)
    val Elevated = Color(0xFF1A1B24)
    val Window = Color(0xFA1A1B22)
    val Glass = Color(0xC921222A)
    val Hairline = Color(0x26FFFFFF)
    val TextPrimary = Color(0xFFF4F2F8)
    val TextSecondary = Color(0xFFAAA7B6)
    val TextTertiary = Color(0xFF777581)
    val Violet = Color(0xFF8B5CF6)
    val VioletBright = Color(0xFFB69CFF)
    val VioletDeep = Color(0xFF4B268E)
    val Success = Color(0xFF7DE2B8)
    val Warning = Color(0xFFF7C56A)
    val Danger = Color(0xFFFF6B68)
}

data class StageDimensions(
    val contentPadding: Dp,
    val topStripHeight: Dp,
    val dockHeight: Dp,
    val dockIconSize: Dp,
    val panelRadius: Dp,
    val windowRadius: Dp,
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
        topStripHeight = (if (compact) 34.dp else 36.dp) * accessibleScale,
        dockHeight = (if (compact) 68.dp else 72.dp) * accessibleScale,
        dockIconSize = (if (compact) 46.dp else 50.dp) * accessibleScale,
        panelRadius = if (compact) 22.dp else 26.dp,
        windowRadius = if (compact) 18.dp else 20.dp,
        gap = if (compact) 12.dp else 16.dp,
        minTouchTarget = 48.dp * accessibleScale,
        sidePanelWidth = if (compact) 0.dp else 296.dp,
    )
}
