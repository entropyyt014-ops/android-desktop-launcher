package com.entropy.stage.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.entropy.stage.design.StageAppIcon
import com.entropy.stage.design.StageSymbol
import com.entropy.stage.design.StageSymbolType
import com.entropy.stage.design.StageToggle
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.launcher.StageSearchField
import kotlinx.coroutines.delay

private sealed interface CommandResult {
    val id: String
    val title: String
    val subtitle: String

    data class Tool(
        override val id: String,
        override val title: String,
        override val subtitle: String,
        val icon: StageSymbolType,
        val action: () -> Unit,
    ) : CommandResult

    data class App(val app: InstalledApp) : CommandResult {
        override val id: String = app.id
        override val title: String = app.label
        override val subtitle: String = app.packageName
    }
}

@Composable
fun CommandCenterOverlay(
    state: StageShellUiState,
    actions: StageShellActions,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val tools = listOf(
        CommandResult.Tool(
            id = "browse",
            title = "Open Browse",
            subtitle = "Desktop-oriented Stage web browser",
            icon = StageSymbolType.BROWSER,
            action = { actions.openSurface(StageSurface.BROWSER) },
        ),
        CommandResult.Tool(
            id = "apps",
            title = "Open Applications",
            subtitle = "Browse every installed app",
            icon = StageSymbolType.APPS,
            action = { actions.openSurface(StageSurface.APP_LIBRARY) },
        ),
        CommandResult.Tool(
            id = "settings",
            title = "Open Stage Settings",
            subtitle = "Appearance, Dock, performance and system",
            icon = StageSymbolType.SETTINGS,
            action = { actions.openSurface(StageSurface.SETTINGS) },
        ),
        CommandResult.Tool(
            id = "home",
            title = "Open Home Settings",
            subtitle = "Choose the default Android launcher",
            icon = StageSymbolType.WINDOW,
            action = { actions.openSystemDestination(SystemDestination.HOME) },
        ),
        CommandResult.Tool(
            id = "wifi",
            title = "Open Wi‑Fi Settings",
            subtitle = "Managed by Android",
            icon = StageSymbolType.WIFI,
            action = { actions.openSystemDestination(SystemDestination.WIFI) },
        ),
    )
    val query = state.searchQuery.trim()
    val matchingTools = tools.filter { result ->
        query.isEmpty() || result.title.contains(query, true) || result.subtitle.contains(query, true)
    }
    val matchingApps = filterInstalledApps(state.apps, query).take(if (query.isEmpty()) 4 else 8)
    val results: List<CommandResult> =
        (matchingTools + matchingApps.map(CommandResult::App)).take(9)
    var selection by remember(query, results.map(CommandResult::id)) { mutableIntStateOf(0) }

    fun runResult(result: CommandResult) {
        when (result) {
            is CommandResult.Tool -> result.action()
            is CommandResult.App -> actions.launchApp(result.app)
        }
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.34f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        Surface(
            modifier = Modifier
                .padding(top = 46.dp, start = 14.dp, end = 14.dp)
                .widthIn(max = 610.dp)
                .fillMaxWidth()
                .shadow(40.dp, RoundedCornerShape(22.dp))
                .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
                .clickable(enabled = false) {},
            color = StagePalette.Window,
            shape = RoundedCornerShape(22.dp),
        ) {
            Column(Modifier.padding(10.dp)) {
                StageSearchField(
                    value = state.searchQuery,
                    onValueChange = actions::setSearchQuery,
                    placeholder = "Search apps, settings and commands",
                    focusRequester = focusRequester,
                    onPreviewKeyEvent = { event ->
                        if (event.type != KeyEventType.KeyDown) return@StageSearchField false
                        when (event.key) {
                            Key.DirectionDown -> {
                                if (results.isNotEmpty()) selection = (selection + 1) % results.size
                                true
                            }

                            Key.DirectionUp -> {
                                if (results.isNotEmpty()) selection =
                                    (selection - 1 + results.size) % results.size
                                true
                            }

                            Key.Enter -> {
                                results.getOrNull(selection)?.let(::runResult)
                                true
                            }

                            Key.Escape -> {
                                onDismiss()
                                true
                            }

                            else -> false
                        }
                    },
                )
                Spacer(Modifier.height(7.dp))
                if (results.isEmpty()) {
                    Text(
                        "No Stage command or installed app matches “${state.searchQuery}”.",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                        color = StagePalette.TextSecondary,
                        fontSize = 12.sp,
                    )
                } else {
                    results.forEachIndexed { index, result ->
                        CommandResultRow(
                            result = result,
                            selected = selection == index,
                            onClick = { runResult(result) },
                        )
                    }
                }
                if (state.deviceProfile.input.hasPhysicalKeyboard) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Text(
                            "↑↓ navigate   ↵ open   esc close",
                            color = StagePalette.TextTertiary,
                            fontSize = 9.5.sp,
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        delay(80)
        focusRequester.requestFocus()
    }
}

@Composable
private fun CommandResultRow(
    result: CommandResult,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) StagePalette.Violet.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.055f))
                .border(1.dp, StagePalette.Hairline, RoundedCornerShape(10.dp))
                .padding(if (result is CommandResult.App) 2.dp else 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            when (result) {
                is CommandResult.Tool -> StageSymbol(result.icon, Modifier.fillMaxSize())
                is CommandResult.App -> StageAppIcon(result.app, Modifier.fillMaxSize())
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(result.title, color = StagePalette.TextPrimary, fontSize = 12.5.sp)
            Text(result.subtitle, color = StagePalette.TextSecondary, fontSize = 10.sp, maxLines = 1)
        }
        if (selected) {
            Text("↵", color = StagePalette.TextTertiary, fontSize = 11.sp)
        }
    }
}

@Composable
fun StageControlCenter(
    state: StageShellUiState,
    actions: StageShellActions,
    onRequestHomeRole: () -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        alignment = Alignment.TopEnd,
        offset = IntOffset(-12, 42),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Surface(
            modifier = Modifier
                .width(286.dp)
                .shadow(28.dp, RoundedCornerShape(20.dp))
                .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(20.dp)),
            color = StagePalette.Window,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Control Center",
                        modifier = Modifier.weight(1f),
                        color = StagePalette.TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                    Text(
                        state.deviceProfile.performanceTier.name.lowercase(),
                        color = StagePalette.TextTertiary,
                        fontSize = 10.sp,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ControlTile(
                        label = "Wi‑Fi",
                        value = if (state.systemStatus.isOnline) "Connected" else "Offline",
                        icon = StageSymbolType.WIFI,
                        active = state.systemStatus.isOnline,
                        onClick = { actions.openSystemDestination(SystemDestination.WIFI) },
                        modifier = Modifier.weight(1f),
                    )
                    ControlTile(
                        label = "Home",
                        value = if (state.homeRoleHeld) "Stage" else "Choose",
                        icon = StageSymbolType.WINDOW,
                        active = state.homeRoleHeld,
                        onClick = onRequestHomeRole,
                        modifier = Modifier.weight(1f),
                    )
                }
                ControlToggleRow(
                    label = "Reduce motion",
                    checked = state.reduceMotion,
                    onCheckedChange = actions::setReduceMotion,
                )
                ControlToggleRow(
                    label = "Desktop texture",
                    checked = state.desktopGrain,
                    onCheckedChange = actions::setDesktopGrain,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    CompactSystemButton("Bluetooth") {
                        actions.openSystemDestination(SystemDestination.BLUETOOTH)
                    }
                    CompactSystemButton("Display") {
                        actions.openSystemDestination(SystemDestination.DISPLAY)
                    }
                    CompactSystemButton("Sound") {
                        actions.openSystemDestination(SystemDestination.SOUND)
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlTile(
    label: String,
    value: String,
    icon: StageSymbolType,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (active) StagePalette.Violet.copy(alpha = 0.2f)
                else Color.White.copy(alpha = 0.04f),
            )
            .border(1.dp, StagePalette.Hairline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        StageSymbol(
            type = icon,
            modifier = Modifier.size(21.dp),
            tint = if (active) StagePalette.VioletBright else StagePalette.TextSecondary,
        )
        Column {
            Text(label, color = StagePalette.TextPrimary, fontSize = 11.5.sp)
            Text(value, color = StagePalette.TextSecondary, fontSize = 9.5.sp)
        }
    }
}

@Composable
private fun ControlToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.035f))
            .border(1.dp, StagePalette.Hairline, RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), color = StagePalette.TextPrimary, fontSize = 11.5.sp)
        StageToggle(checked, onCheckedChange)
    }
}

@Composable
private fun CompactSystemButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, StagePalette.Hairline, RoundedCornerShape(9.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = StagePalette.TextSecondary, fontSize = 9.5.sp)
    }
}
