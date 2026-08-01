package com.entropy.stage.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropy.stage.design.StageGlyph
import com.entropy.stage.design.StageToggle
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.shell.StageShellActions
import com.entropy.stage.shell.StageShellUiState
import com.entropy.stage.shell.SystemDestination
import kotlin.math.roundToInt

private enum class SettingsSection(val label: String) {
    APPEARANCE("Appearance"),
    DESKTOP("Desktop & Dock"),
    PERFORMANCE("Performance"),
    SYSTEM("System"),
    ABOUT("About"),
}

@Composable
fun StageSettingsPanel(
    state: StageShellUiState,
    actions: StageShellActions,
    onRequestHomeRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(SettingsSection.APPEARANCE) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val showSidebar = maxWidth >= 560.dp
        if (showSidebar) {
            Row(Modifier.fillMaxSize()) {
                SettingsSidebar(
                    selected = selected,
                    onSelect = { selected = it },
                    modifier = Modifier
                        .width(168.dp)
                        .fillMaxHeight(),
                )
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(StagePalette.Hairline),
                )
                SettingsContent(
                    selected = selected,
                    state = state,
                    actions = actions,
                    onRequestHomeRole = onRequestHomeRole,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                CompactSectionBar(
                    selected = selected,
                    onSelect = { selected = it },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(StagePalette.Hairline),
                )
                SettingsContent(
                    selected = selected,
                    state = state,
                    actions = actions,
                    onRequestHomeRole = onRequestHomeRole,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SettingsSidebar(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.018f))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StageGlyph(Modifier.size(21.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                "Stage Settings",
                color = StagePalette.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        SettingsSection.entries.forEach { section ->
            SectionButton(
                label = section.label,
                selected = selected == section,
                onClick = { onSelect(section) },
            )
        }
    }
}

@Composable
private fun CompactSectionBar(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SettingsSection.entries.forEach { section ->
            SectionButton(
                label = section.label,
                selected = selected == section,
                onClick = { onSelect(section) },
            )
        }
    }
}

@Composable
private fun SectionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selected) StagePalette.Violet.copy(alpha = 0.2f) else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        color = if (selected) StagePalette.TextPrimary else StagePalette.TextSecondary,
        fontSize = 11.5.sp,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        maxLines = 1,
    )
}

@Composable
private fun SettingsContent(
    selected: SettingsSection,
    state: StageShellUiState,
    actions: StageShellActions,
    onRequestHomeRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = selected.label,
                color = StagePalette.TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.25).sp,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = sectionDescription(selected),
                color = StagePalette.TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
            )
        }

        when (selected) {
            SettingsSection.APPEARANCE -> {
                item {
                    SettingsGroup(title = "Liquid Graphite") {
                        AccentPreview()
                        SettingToggleRow(
                            title = "Reduce motion",
                            subtitle = "Shortens workspace and Dock animations",
                            checked = state.reduceMotion,
                            onCheckedChange = actions::setReduceMotion,
                        )
                        SettingToggleRow(
                            title = "Desktop texture",
                            subtitle = "Adds subtle deterministic grain to the wallpaper",
                            checked = state.desktopGrain,
                            onCheckedChange = actions::setDesktopGrain,
                        )
                    }
                }
            }

            SettingsSection.DESKTOP -> {
                item {
                    SettingsGroup(title = "Dock") {
                        SettingToggleRow(
                            title = "Icon magnification",
                            subtitle = "Lift icons under a mouse or press",
                            checked = state.dockMagnification,
                            onCheckedChange = actions::setDockMagnification,
                        )
                        InfoRow("Pinned applications", state.pinnedApps.size.toString())
                        InfoRow("Installed applications", state.apps.size.toString())
                    }
                }
                item {
                    SettingsGroup(title = "Stage windows") {
                        InfoRow("Portrait", "One dominant surface")
                        InfoRow("Landscape", "Floating desktop window")
                        InfoRow("Window restoration", "On")
                    }
                }
            }

            SettingsSection.PERFORMANCE -> {
                item {
                    SettingsGroup(title = "Renderer") {
                        StatusRow(
                            title = "${state.deviceProfile.performanceTier.name.lowercase().replaceFirstChar { it.uppercase() }} profile",
                            subtitle = if (state.deviceProfile.budget.allowContinuousBlur) {
                                "Full depth effects available"
                            } else {
                                "Cached translucency • no continuous full-screen blur"
                            },
                        )
                        InfoRow("Live WebView budget", state.deviceProfile.budget.maxLiveWebViews.toString())
                        InfoRow("Active window budget", state.deviceProfile.budget.maxSimultaneousWindows.toString())
                    }
                }
                item {
                    SettingsGroup(title = "Device information") {
                        InfoRow("Device", "${state.deviceProfile.manufacturer} ${state.deviceProfile.model}")
                        InfoRow("Android API", state.deviceProfile.sdkInt.toString())
                        InfoRow("Viewport", "${state.deviceProfile.widthDp} × ${state.deviceProfile.heightDp} dp")
                        InfoRow("Physical RAM", formatRam(state.deviceProfile.totalRamMb))
                        InfoRow("App heap class", "${state.deviceProfile.appMemoryClassMb} MB")
                        InfoRow(
                            "Input",
                            buildList {
                                if (state.deviceProfile.input.hasTouchscreen) add("Touch")
                                if (state.deviceProfile.input.hasMouse) add("Mouse")
                                if (state.deviceProfile.input.hasPhysicalKeyboard) add("Keyboard")
                            }.ifEmpty { listOf("Unknown") }.joinToString(" + "),
                        )
                    }
                }
            }

            SettingsSection.SYSTEM -> {
                item {
                    SettingsGroup(title = "Default Home") {
                        StatusRow(
                            title = if (state.homeRoleHeld) "Stage is the Home app" else "Stage is not the Home app",
                            subtitle = "Android always confirms launcher changes",
                            positive = state.homeRoleHeld,
                        )
                        ActionRow(
                            label = if (state.homeRoleHeld) "Open Home settings" else "Choose Stage as Home",
                            onClick = onRequestHomeRole,
                        )
                    }
                }
                item {
                    SettingsGroup(title = "Android controls") {
                        ActionRow("Wi‑Fi settings") { actions.openSystemDestination(SystemDestination.WIFI) }
                        ActionRow("Bluetooth settings") { actions.openSystemDestination(SystemDestination.BLUETOOTH) }
                        ActionRow("Display settings") { actions.openSystemDestination(SystemDestination.DISPLAY) }
                        ActionRow("Sound settings") { actions.openSystemDestination(SystemDestination.SOUND) }
                    }
                }
            }

            SettingsSection.ABOUT -> {
                item {
                    SettingsGroup(title = "Stage Desktop") {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(StagePalette.Violet.copy(alpha = 0.13f))
                                    .border(1.dp, StagePalette.Hairline, RoundedCornerShape(15.dp))
                                    .padding(12.dp),
                            ) {
                                StageGlyph(Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Stage Desktop",
                                    color = StagePalette.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    "0.2.0 • Gate 1",
                                    color = StagePalette.TextSecondary,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        InfoRow("Architecture", "Native Kotlin + Compose")
                        InfoRow("Minimum Android", "11 (API 30)")
                        InfoRow("Storage", "Local only")
                    }
                }
                item {
                    Text(
                        text = "Stage can host its own tools and launch Android apps. Android does not allow arbitrary third‑party apps or protected System UI to be embedded inside Stage windows.",
                        color = StagePalette.TextSecondary,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title.uppercase(),
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            color = StagePalette.TextTertiary,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.9.sp,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.035f))
                .border(1.dp, StagePalette.Hairline, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = StagePalette.TextPrimary, fontSize = 12.5.sp)
            Text(subtitle, color = StagePalette.TextSecondary, fontSize = 10.5.sp)
        }
        Spacer(Modifier.width(12.dp))
        StageToggle(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f), color = StagePalette.TextSecondary, fontSize = 11.5.sp)
        Text(
            value,
            modifier = Modifier.widthIn(max = 220.dp),
            color = StagePalette.TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            fontSize = 11.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StatusRow(
    title: String,
    subtitle: String,
    positive: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (positive) StagePalette.Success else StagePalette.Warning),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = StagePalette.TextPrimary, fontSize = 12.5.sp)
            Text(subtitle, color = StagePalette.TextSecondary, fontSize = 10.5.sp)
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), color = StagePalette.TextPrimary, fontSize = 12.sp)
        Text("›", color = StagePalette.TextTertiary, fontSize = 18.sp)
    }
}

@Composable
private fun AccentPreview() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Accent", modifier = Modifier.weight(1f), color = StagePalette.TextSecondary, fontSize = 11.5.sp)
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(StagePalette.Violet)
                .border(2.dp, Color.White, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text("Stage violet", color = StagePalette.TextPrimary, fontSize = 11.5.sp)
    }
}

private fun sectionDescription(section: SettingsSection): String = when (section) {
    SettingsSection.APPEARANCE -> "Tune visual depth and motion without sacrificing readability."
    SettingsSection.DESKTOP -> "Control Stage windows, installed apps, and Dock behavior."
    SettingsSection.PERFORMANCE -> "Real device measurements and the renderer budget selected for this phone."
    SettingsSection.SYSTEM -> "Open Android-controlled settings safely."
    SettingsSection.ABOUT -> "Version, architecture, storage, and platform boundaries."
}

private fun formatRam(totalRamMb: Long): String = when {
    totalRamMb <= 0 -> "Unknown"
    totalRamMb >= 1_024 -> "${(totalRamMb / 1_024f * 10).roundToInt() / 10f} GB"
    else -> "$totalRamMb MB"
}
