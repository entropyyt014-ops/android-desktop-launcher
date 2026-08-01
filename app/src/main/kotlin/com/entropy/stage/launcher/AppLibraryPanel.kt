package com.entropy.stage.launcher

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.entropy.stage.design.StageAppIcon
import com.entropy.stage.design.StageSymbol
import com.entropy.stage.design.StageSymbolType
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.shell.InstalledApp
import com.entropy.stage.shell.StageShellActions
import com.entropy.stage.shell.StageShellUiState
import com.entropy.stage.shell.filterInstalledApps

@Composable
fun AppLibraryPanel(
    state: StageShellUiState,
    actions: StageShellActions,
    modifier: Modifier = Modifier,
    compactLayout: Boolean = false,
) {
    val filtered = remember(state.apps, state.searchQuery) {
        filterInstalledApps(state.apps, state.searchQuery)
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = if (compactLayout) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Applications",
                color = StagePalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = when {
                    state.appsLoading -> "Updating…"
                    else -> "${filtered.size} apps"
                },
                color = StagePalette.TextTertiary,
                fontSize = 11.sp,
            )
        }

        StageSearchField(
            value = state.searchQuery,
            onValueChange = actions::setSearchQuery,
            placeholder = "Search applications",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(if (compactLayout) 4.dp else 8.dp))

        when {
            state.appsLoading && state.apps.isEmpty() -> EmptyLibraryMessage(
                title = "Reading installed apps…",
                subtitle = "Stage only shows real launchable apps on this device.",
            )

            filtered.isEmpty() -> EmptyLibraryMessage(
                title = "No matching apps",
                subtitle = "Try another name or package.",
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = if (compactLayout) 68.dp else 76.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 12.dp,
                    vertical = if (compactLayout) 6.dp else 10.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(if (compactLayout) 4.dp else 8.dp),
            ) {
                items(
                    items = filtered,
                    key = InstalledApp::id,
                ) { app ->
                    AppTile(
                        app = app,
                        pinned = app.id in state.pinnedAppIds,
                        onOpen = { actions.launchApp(app) },
                        onTogglePin = { actions.togglePinned(app) },
                        onAppInfo = { actions.openAppInfo(app) },
                        onUninstall = { actions.requestUninstall(app) },
                        compact = compactLayout,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: InstalledApp,
    pinned: Boolean,
    onOpen: () -> Unit,
    onTogglePin: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    compact: Boolean,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .combinedClickable(
                    role = Role.Button,
                    onClick = onOpen,
                    onLongClick = { menuOpen = true },
                )
                .padding(horizontal = 4.dp, vertical = if (compact) 4.dp else 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                StageAppIcon(
                    app = app,
                    modifier = Modifier.size(if (compact) 42.dp else 52.dp),
                )
                if (pinned) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(StagePalette.VioletBright)
                            .border(2.dp, StagePalette.Window, CircleShape),
                    )
                }
            }
            Spacer(Modifier.height(if (compact) 4.dp else 7.dp))
            Text(
                text = app.label,
                color = StagePalette.TextPrimary,
                fontSize = if (compact) 9.5.sp else 10.5.sp,
                lineHeight = if (compact) 11.sp else 13.sp,
                textAlign = TextAlign.Center,
                maxLines = if (compact) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            containerColor = StagePalette.Elevated,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, StagePalette.Hairline),
        ) {
            AppMenuItem("Open") {
                menuOpen = false
                onOpen()
            }
            AppMenuItem(if (pinned) "Remove from Dock" else "Keep in Dock") {
                menuOpen = false
                onTogglePin()
            }
            AppMenuItem("App information") {
                menuOpen = false
                onAppInfo()
            }
            AppMenuItem("Uninstall…", tint = StagePalette.Danger) {
                menuOpen = false
                onUninstall()
            }
        }
    }
}

@Composable
private fun AppMenuItem(
    label: String,
    tint: Color = StagePalette.TextPrimary,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, color = tint, fontSize = 12.sp) },
        onClick = onClick,
    )
}

@Composable
private fun EmptyLibraryMessage(
    title: String,
    subtitle: String,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StageSymbol(
            type = StageSymbolType.APPS,
            modifier = Modifier.size(34.dp),
            tint = StagePalette.TextTertiary,
        )
        Spacer(Modifier.height(12.dp))
        Text(title, color = StagePalette.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            color = StagePalette.TextSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun StageSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean)? = null,
) {
    val fieldModifier = Modifier
        .fillMaxWidth()
        .height(38.dp)
    val inputModifier = modifier
        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
        .then(if (onPreviewKeyEvent != null) Modifier.onPreviewKeyEvent(onPreviewKeyEvent) else Modifier)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = inputModifier,
        singleLine = true,
        textStyle = TextStyle(
            color = StagePalette.TextPrimary,
            fontSize = 13.sp,
        ),
        cursorBrush = Brush.verticalGradient(
            listOf(StagePalette.VioletBright, StagePalette.VioletBright),
        ),
        visualTransformation = VisualTransformation.None,
        decorationBox = { innerTextField ->
            Row(
                modifier = fieldModifier
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .border(1.dp, StagePalette.Hairline, RoundedCornerShape(11.dp))
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StageSymbol(
                    type = StageSymbolType.SEARCH,
                    modifier = Modifier.size(16.dp),
                    tint = StagePalette.TextTertiary,
                )
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(placeholder, color = StagePalette.TextTertiary, fontSize = 12.sp)
                    }
                    innerTextField()
                }
                if (value.isNotEmpty()) {
                    Text(
                        text = "×",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onValueChange("") }
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        color = StagePalette.TextSecondary,
                        fontSize = 16.sp,
                    )
                }
            }
        },
    )
}
