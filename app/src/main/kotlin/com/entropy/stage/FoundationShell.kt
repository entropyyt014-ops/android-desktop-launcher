package com.entropy.stage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropy.stage.designsystem.StageDimensions
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.designsystem.stageDimensionsFor
import com.entropy.stage.device.DeviceProfile
import kotlin.math.roundToInt

@Composable
fun FoundationShell(
    profile: DeviceProfile,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF08090D),
                        Color(0xFF0C0D14),
                        Color(0xFF07080C),
                    ),
                ),
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("stage-shell"),
    ) {
        val fontScale = LocalDensity.current.fontScale
        val dimensions = remember(maxWidth, fontScale) {
            stageDimensionsFor(width = maxWidth, fontScale = fontScale)
        }
        val useSidePanel = maxWidth >= 700.dp && maxWidth > maxHeight

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(dimensions.contentPadding),
            verticalArrangement = Arrangement.spacedBy(dimensions.gap),
        ) {
            TopStrip(
                profile = profile,
                dimensions = dimensions,
                modifier = Modifier.fillMaxWidth(),
            )

            if (useSidePanel) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(dimensions.gap),
                ) {
                    WorkspaceSurface(
                        profile = profile,
                        dimensions = dimensions,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    ProfilePanel(
                        profile = profile,
                        dimensions = dimensions,
                        modifier = Modifier
                            .width(dimensions.sidePanelWidth)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(dimensions.gap),
                ) {
                    WorkspaceSurface(
                        profile = profile,
                        dimensions = dimensions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    ProfilePanel(
                        profile = profile,
                        dimensions = dimensions,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            FoundationDock(
                dimensions = dimensions,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TopStrip(
    profile: DeviceProfile,
    dimensions: StageDimensions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(dimensions.topStripHeight),
        color = StagePalette.Graphite.copy(alpha = 0.92f),
        shape = RoundedCornerShape(dimensions.panelRadius),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(StagePalette.Violet),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Stage",
                color = StagePalette.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
            Text(
                text = "  /  Foundation",
                color = StagePalette.TextSecondary,
                fontSize = 13.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "API ${profile.sdkInt}  •  ${profile.performanceTier.name}",
                color = StagePalette.TextSecondary,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun WorkspaceSurface(
    profile: DeviceProfile,
    dimensions: StageDimensions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = StagePalette.Graphite,
        shape = RoundedCornerShape(dimensions.panelRadius),
        tonalElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            StagePalette.Violet.copy(alpha = 0.22f),
                            StagePalette.Graphite.copy(alpha = 0f),
                        ),
                        center = Offset(180f, 90f),
                        radius = 650f,
                    ),
                )
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = StagePalette.Violet.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(100.dp),
                ) {
                    Text(
                        text = "GATE 0",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = StagePalette.VioletBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    )
                }
                Text(
                    text = "Adaptive shell online",
                    color = StagePalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp,
                )
                Text(
                    text = "${profile.manufacturer} ${profile.model}",
                    color = StagePalette.TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Text(
                text = "No workspace open",
                modifier = Modifier.align(Alignment.BottomStart),
                color = StagePalette.TextSecondary.copy(alpha = 0.75f),
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun ProfilePanel(
    profile: DeviceProfile,
    dimensions: StageDimensions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.testTag("device-profile"),
        color = StagePalette.Elevated.copy(alpha = 0.9f),
        shape = RoundedCornerShape(dimensions.panelRadius),
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Device profile",
                    modifier = Modifier.weight(1f),
                    color = StagePalette.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                StatusBadge(text = "MEASURED")
            }
            MetricRow(
                label = "Viewport",
                value = "${profile.widthDp} × ${profile.heightDp} dp",
            )
            MetricRow(
                label = "Memory",
                value = "${formatRam(profile.totalRamMb)} • ${profile.appMemoryClassMb} MB app",
            )
            MetricRow(
                label = "Workspace",
                value = "${profile.posture.name.replace('_', ' ')} • ${profile.windowClass.name}",
            )
            MetricRow(
                label = "Input",
                value = inputSummary(profile),
            )
            MetricRow(
                label = "Lean budget",
                value = "${profile.budget.maxLiveWebViews} live web views • " +
                    "${profile.budget.maxSimultaneousWindows} windows",
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String) {
    Box(
        modifier = Modifier
            .border(
                width = 1.dp,
                color = StagePalette.Success.copy(alpha = 0.55f),
                shape = RoundedCornerShape(100.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = StagePalette.Success,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.width(74.dp),
            color = StagePalette.TextSecondary,
            fontSize = 11.sp,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            color = StagePalette.TextPrimary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FoundationDock(
    dimensions: StageDimensions,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(dimensions.dockHeight),
        color = StagePalette.Graphite.copy(alpha = 0.96f),
        shape = RoundedCornerShape(dimensions.panelRadius),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(
                space = 10.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .size(dimensions.minTouchTarget)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            if (index == 2) {
                                StagePalette.Violet.copy(alpha = 0.3f)
                            } else {
                                StagePalette.Elevated
                            },
                        )
                        .border(
                            width = 1.dp,
                            color = if (index == 2) {
                                StagePalette.Violet.copy(alpha = 0.5f)
                            } else {
                                StagePalette.Hairline
                            },
                            shape = RoundedCornerShape(15.dp),
                        ),
                )
            }
        }
    }
}

private fun formatRam(totalRamMb: Long): String {
    if (totalRamMb <= 0) return "Unknown"
    return if (totalRamMb >= 1_024) {
        "${(totalRamMb / 1_024f).roundToInt()} GB"
    } else {
        "$totalRamMb MB"
    }
}

private fun inputSummary(profile: DeviceProfile): String = buildList {
    if (profile.input.hasTouchscreen) add("Touch")
    if (profile.input.hasMouse) add("Mouse")
    if (profile.input.hasPhysicalKeyboard) add("Keyboard")
}.ifEmpty { listOf("No direct input") }.joinToString(" + ")
