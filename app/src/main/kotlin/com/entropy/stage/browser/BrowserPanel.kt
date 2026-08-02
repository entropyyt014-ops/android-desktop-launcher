package com.entropy.stage.browser

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import com.entropy.stage.designsystem.StagePalette
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun StageBrowserPanel(
    state: BrowserUiState,
    actions: BrowserActions,
    hasPhysicalKeyboard: Boolean,
    modifier: Modifier = Modifier,
) {
    val addressFocusRequester = remember { FocusRequester() }
    val findFocusRequester = remember { FocusRequester() }
    val inspectionMode = LocalInspectionMode.current
    val activeTab = state.activeTab
    val command = state.command

    LaunchedEffect(command?.serial) {
        val pending = command ?: return@LaunchedEffect
        if (pending.type == BrowserCommandType.FOCUS_ADDRESS) {
            addressFocusRequester.requestFocus()
            actions.consumeBrowserCommand(pending.serial)
        }
    }
    LaunchedEffect(state.findBarVisible) {
        if (state.findBarVisible) {
            delay(70)
            findFocusRequester.requestFocus()
        }
    }
    LaunchedEffect(state.panel, state.downloads.map { it.status }) {
        if (state.panel == BrowserPanel.DOWNLOADS && state.downloads.any { it.isInProgress }) {
            delay(1_000)
            actions.refreshBrowserDownloads()
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Color(0xFF101116))) {
        val compactPanel = maxWidth < 520.dp
        Column(Modifier.fillMaxSize()) {
            if (!state.focusMode) {
                BrowserTabStrip(state = state, actions = actions)
                BrowserToolbar(
                    state = state,
                    actions = actions,
                    addressFocusRequester = addressFocusRequester,
                )
                if (state.findBarVisible) {
                    BrowserFindBar(
                        state = state,
                        actions = actions,
                        focusRequester = findFocusRequester,
                    )
                }
                if (activeTab.isLoading) {
                    Box(
                        Modifier
                            .fillMaxWidth(activeTab.progress.coerceIn(2, 100) / 100f)
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(StagePalette.Violet, StagePalette.VioletBright),
                                ),
                            ),
                    )
                }
            }

            Box(Modifier.fillMaxSize()) {
                if (inspectionMode) {
                    BrowserPreviewPage(Modifier.fillMaxSize())
                } else {
                    BrowserWebSurface(
                        state = state,
                        actions = actions,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                if (state.focusMode) {
                    FocusModePill(
                        tab = activeTab,
                        onExit = { actions.setBrowserFocusMode(false) },
                        modifier = Modifier.align(Alignment.TopCenter),
                    )
                }

                if (state.panel != BrowserPanel.NONE) {
                    BrowserSidePanel(
                        state = state,
                        actions = actions,
                        hasPhysicalKeyboard = hasPhysicalKeyboard,
                        compact = compactPanel,
                    )
                }

                activeTab.lastError?.let { error ->
                    BrowserErrorCard(
                        message = error,
                        onReload = { actions.requestBrowserCommand(BrowserCommandType.RELOAD) },
                        onOpenExternally = actions::openActiveBrowserPageExternally,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }

    state.pendingExternalUrl?.let { url ->
        ExternalLinkDialog(
            url = url,
            onCancel = actions::dismissExternalNavigation,
            onOpen = actions::confirmExternalNavigation,
        )
    }
}

@Composable
private fun BrowserTabStrip(
    state: BrowserUiState,
    actions: BrowserActions,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color(0xFF17181E))
            .border(0.5.dp, StagePalette.Hairline),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .padding(start = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.tabs.forEach { tab ->
                BrowserTabPill(
                    tab = tab,
                    selected = tab.id == state.activeTabId,
                    onSelect = { actions.selectBrowserTab(tab.id) },
                    onClose = { actions.closeBrowserTab(tab.id) },
                )
            }
        }
        BrowserChromeButton(label = "New tab", text = "+") { actions.newBrowserTab() }
        BrowserChromeButton(label = "Tab overview", text = "▦") {
            actions.setBrowserPanel(BrowserPanel.TABS)
        }
        Spacer(Modifier.width(4.dp))
    }
}

@Composable
private fun BrowserTabPill(
    tab: BrowserTab,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .widthIn(min = 116.dp, max = 168.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(if (selected) Color(0xFF282832) else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onSelect)
            .padding(start = 9.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    when {
                        tab.isLoading -> StagePalette.VioletBright
                        tab.lastError != null -> StagePalette.Danger
                        else -> StagePalette.TextTertiary
                    },
                ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = tab.title.ifBlank { "New Tab" },
            modifier = Modifier.weight(1f),
            color = if (selected) StagePalette.TextPrimary else StagePalette.TextSecondary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "×",
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClose)
                .padding(horizontal = 5.dp, vertical = 3.dp),
            color = StagePalette.TextSecondary,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun BrowserToolbar(
    state: BrowserUiState,
    actions: BrowserActions,
    addressFocusRequester: FocusRequester,
) {
    val tab = state.activeTab
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(0xF51A1B21))
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        BrowserChromeButton(
            label = "Back",
            text = "‹",
            enabled = tab.canGoBack,
        ) { actions.requestBrowserCommand(BrowserCommandType.BACK) }
        BrowserChromeButton(
            label = "Forward",
            text = "›",
            enabled = tab.canGoForward,
        ) { actions.requestBrowserCommand(BrowserCommandType.FORWARD) }
        BrowserChromeButton(
            label = if (tab.isLoading) "Stop" else "Reload",
            text = if (tab.isLoading) "×" else "↻",
        ) {
            actions.requestBrowserCommand(
                if (tab.isLoading) BrowserCommandType.STOP else BrowserCommandType.RELOAD,
            )
        }
        BrowserAddressField(
            value = state.addressInput,
            secure = tab.url.startsWith("https://"),
            onValueChange = actions::updateBrowserAddress,
            onSubmit = actions::submitBrowserAddress,
            focusRequester = addressFocusRequester,
            modifier = Modifier.weight(1f),
        )
        BrowserChromeButton(
            label = if (state.activePageBookmarked) "Remove bookmark" else "Bookmark page",
            text = if (state.activePageBookmarked) "★" else "☆",
            highlighted = state.activePageBookmarked,
        ) { actions.toggleActiveBrowserBookmark() }
        BrowserChromeButton(
            label = "${tab.profile.label} site profile",
            text = tab.profile.label.first().toString(),
            highlighted = tab.profile == BrowserProfile.DESKTOP,
        ) { actions.setBrowserPanel(BrowserPanel.PROFILES) }
        BrowserChromeButton(label = "Downloads", text = "⇩") {
            actions.setBrowserPanel(BrowserPanel.DOWNLOADS)
        }
        BrowserChromeButton(label = "Browser menu", text = "•••") {
            actions.setBrowserPanel(BrowserPanel.MENU)
        }
    }
}

@Composable
private fun BrowserAddressField(
    value: String,
    secure: Boolean,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(33.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused },
        singleLine = true,
        textStyle = TextStyle(
            color = StagePalette.TextPrimary,
            fontSize = 11.5.sp,
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Go,
        ),
        keyboardActions = KeyboardActions(onGo = { onSubmit() }),
        cursorBrush = Brush.verticalGradient(listOf(StagePalette.VioletBright, StagePalette.VioletBright)),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF101116))
                    .border(
                        1.dp,
                        if (focused) StagePalette.Violet.copy(alpha = 0.65f)
                        else StagePalette.Hairline,
                        RoundedCornerShape(10.dp),
                    )
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (secure) "●" else "!",
                    color = if (secure) StagePalette.Success else StagePalette.Warning,
                    fontSize = 8.sp,
                )
                Spacer(Modifier.width(7.dp))
                Box(Modifier.weight(1f)) { inner() }
                if (value.isNotEmpty() && focused) {
                    Text(
                        text = "×",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { onValueChange("") }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        color = StagePalette.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        },
    )
}

@Composable
private fun BrowserFindBar(
    state: BrowserUiState,
    actions: BrowserActions,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFF1A1B21))
            .border(0.5.dp, StagePalette.Hairline)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        BasicTextField(
            value = state.findQuery,
            onValueChange = actions::setBrowserFindQuery,
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .focusRequester(focusRequester)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF101116))
                .border(1.dp, StagePalette.Hairline, RoundedCornerShape(8.dp))
                .padding(horizontal = 9.dp, vertical = 6.dp),
            singleLine = true,
            textStyle = TextStyle(color = StagePalette.TextPrimary, fontSize = 11.sp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { actions.requestBrowserCommand(BrowserCommandType.FIND_NEXT) },
            ),
        )
        BrowserChromeButton("Previous match", "↑") {
            actions.requestBrowserCommand(BrowserCommandType.FIND_PREVIOUS)
        }
        BrowserChromeButton("Next match", "↓") {
            actions.requestBrowserCommand(BrowserCommandType.FIND_NEXT)
        }
        BrowserChromeButton("Close find", "×") { actions.setBrowserFindVisible(false) }
    }
}

@Composable
private fun BrowserChromeButton(
    label: String,
    text: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(31.dp)
            .semantics { contentDescription = label }
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (highlighted) StagePalette.Violet.copy(alpha = 0.22f)
                else Color.Transparent,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (enabled) {
                if (highlighted) StagePalette.VioletBright else StagePalette.TextPrimary
            } else {
                StagePalette.TextTertiary.copy(alpha = 0.45f)
            },
            fontSize = if (text.length > 1) 9.5.sp else 17.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}

@Composable
private fun FocusModePill(
    tab: BrowserTab,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = remember(tab.url) { runCatching { tab.url.toUri().host }.getOrNull() ?: tab.url }
    Row(
        modifier = modifier
            .padding(top = 8.dp)
            .shadow(18.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xEC17181E))
            .border(1.dp, StagePalette.Hairline, RoundedCornerShape(18.dp))
            .clickable(onClick = onExit)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(StagePalette.Success))
        Spacer(Modifier.width(7.dp))
        Text(
            text = host,
            color = StagePalette.TextPrimary,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(9.dp))
        Text("Exit focus", color = StagePalette.VioletBright, fontSize = 9.5.sp)
    }
}

@Composable
private fun BrowserSidePanel(
    state: BrowserUiState,
    actions: BrowserActions,
    hasPhysicalKeyboard: Boolean,
    compact: Boolean,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f))
            .clickable { actions.setBrowserPanel(BrowserPanel.NONE) },
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .then(if (compact) Modifier.fillMaxWidth(0.9f) else Modifier.width(330.dp))
                .shadow(28.dp, RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                .clickable(enabled = false) {},
            color = Color(0xFC191A20),
            shape = RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp),
        ) {
            Column(Modifier.fillMaxSize()) {
                PanelHeader(
                    title = panelTitle(state.panel),
                    subtitle = if (hasPhysicalKeyboard) "Keyboard shortcuts are active" else null,
                    onClose = { actions.setBrowserPanel(BrowserPanel.NONE) },
                )
                when (state.panel) {
                    BrowserPanel.MENU -> BrowserMenu(state, actions)
                    BrowserPanel.TABS -> BrowserTabsOverview(state, actions)
                    BrowserPanel.PROFILES -> BrowserProfiles(state, actions)
                    BrowserPanel.BOOKMARKS -> BrowserBookmarks(state, actions)
                    BrowserPanel.HISTORY -> BrowserHistory(state, actions)
                    BrowserPanel.DOWNLOADS -> BrowserDownloads(state, actions)
                    BrowserPanel.NONE -> Unit
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(
    title: String,
    subtitle: String?,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .border(0.5.dp, StagePalette.Hairline)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = StagePalette.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            subtitle?.let { Text(it, color = StagePalette.TextTertiary, fontSize = 9.sp) }
        }
        BrowserChromeButton("Close panel", "×", onClick = onClose)
    }
}

@Composable
private fun BrowserMenu(state: BrowserUiState, actions: BrowserActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        item {
            PanelAction("New tab", "Ctrl/Meta + T") { actions.newBrowserTab() }
            PanelAction("Duplicate tab", state.activeTab.url) {
                actions.duplicateBrowserTab(state.activeTabId)
            }
            PanelAction("Reopen closed tab", "Ctrl/Meta + Shift + T") {
                actions.reopenClosedBrowserTab()
            }
            PanelDivider()
            PanelAction("Find in page", "Ctrl/Meta + F") { actions.setBrowserFindVisible(true) }
            PanelAction("Focus mode", "Collapse browser chrome") { actions.setBrowserFocusMode(true) }
            PanelAction("Share page", state.activeTab.title) { actions.shareActiveBrowserPage() }
            PanelAction("Open in Android browser", hostLabel(state.activeTab.url)) {
                actions.openActiveBrowserPageExternally()
            }
            PanelDivider()
            PanelAction("Bookmarks", "${state.bookmarks.size} saved") {
                actions.setBrowserPanel(BrowserPanel.BOOKMARKS)
            }
            PanelAction("History", "${state.history.size} recent sites") {
                actions.setBrowserPanel(BrowserPanel.HISTORY)
            }
            PanelAction("Downloads", "${state.downloads.size} transfers") {
                actions.setBrowserPanel(BrowserPanel.DOWNLOADS)
            }
            PanelAction("Site profile", state.activeTab.profile.label) {
                actions.setBrowserPanel(BrowserPanel.PROFILES)
            }
        }
    }
}

@Composable
private fun BrowserTabsOverview(state: BrowserUiState, actions: BrowserActions) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(9.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        items(state.tabs, key = BrowserTab::id) { tab ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (tab.id == state.activeTabId) StagePalette.Violet.copy(alpha = 0.16f)
                        else Color.White.copy(alpha = 0.025f),
                    )
                    .clickable { actions.selectBrowserTab(tab.id) }
                    .padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(27.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(tab.profile.label.first().toString(), color = StagePalette.VioletBright, fontSize = 11.sp)
                }
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(tab.title, color = StagePalette.TextPrimary, fontSize = 11.5.sp, maxLines = 1)
                    Text(tab.url, color = StagePalette.TextTertiary, fontSize = 9.5.sp, maxLines = 1)
                }
                BrowserChromeButton("Close ${tab.title}", "×") { actions.closeBrowserTab(tab.id) }
            }
        }
        item { PanelAction("New tab", BROWSER_HOME_URL) { actions.newBrowserTab() } }
    }
}

@Composable
private fun BrowserProfiles(state: BrowserUiState, actions: BrowserActions) {
    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BrowserProfile.entries.forEach { profile ->
            val description = when (profile) {
                BrowserProfile.DESKTOP -> "Desktop Chromium identity, wide viewport and overview scaling"
                BrowserProfile.ADAPTIVE -> "Android identity with a wide responsive viewport"
                BrowserProfile.MOBILE -> "Standard Android mobile rendering"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (state.activeTab.profile == profile) StagePalette.Violet.copy(alpha = 0.18f)
                        else Color.White.copy(alpha = 0.025f),
                    )
                    .border(1.dp, StagePalette.Hairline, RoundedCornerShape(13.dp))
                    .clickable { actions.setBrowserProfile(profile) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(31.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(StagePalette.Violet.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(profile.label.first().toString(), color = StagePalette.VioletBright, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(profile.label, color = StagePalette.TextPrimary, fontSize = 12.sp)
                    Text(description, color = StagePalette.TextSecondary, fontSize = 9.5.sp, lineHeight = 13.sp)
                }
            }
        }
        Text(
            text = "Stage remembers this choice for ${hostLabel(state.activeTab.url)}. A desktop profile cannot override every site's own device detection.",
            modifier = Modifier.padding(7.dp),
            color = StagePalette.TextTertiary,
            fontSize = 9.5.sp,
            lineHeight = 13.sp,
        )
    }
}

@Composable
private fun BrowserBookmarks(state: BrowserUiState, actions: BrowserActions) {
    if (state.bookmarks.isEmpty()) {
        EmptyPanel("No bookmarks yet", "Use ☆ beside the address field to save a page.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(state.bookmarks, key = BrowserBookmark::url) { bookmark ->
            BrowserLibraryRow(
                title = bookmark.title,
                subtitle = bookmark.url,
                onOpen = { actions.navigateBrowser(bookmark.url) },
                trailingLabel = "Remove",
                onTrailing = { actions.removeBrowserBookmark(bookmark.url) },
            )
        }
    }
}

@Composable
private fun BrowserHistory(state: BrowserUiState, actions: BrowserActions) {
    if (state.history.isEmpty()) {
        EmptyPanel("No history yet", "Completed HTTPS pages will appear here.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(9.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(state.history, key = BrowserHistoryEntry::url) { entry ->
            BrowserLibraryRow(
                title = entry.title,
                subtitle = "${entry.url} · ${formatTime(entry.visitedAt)}",
                onOpen = { actions.navigateBrowser(entry.url) },
            )
        }
    }
}

@Composable
private fun BrowserDownloads(state: BrowserUiState, actions: BrowserActions) {
    if (state.downloads.isEmpty()) {
        EmptyPanel("No downloads", "Files requested by a webpage will use Android DownloadManager.")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(state.downloads, key = BrowserDownload::id) { download ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Color.White.copy(alpha = 0.028f))
                    .border(1.dp, StagePalette.Hairline, RoundedCornerShape(13.dp))
                    .padding(11.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(download.fileName, color = StagePalette.TextPrimary, fontSize = 11.5.sp, maxLines = 1)
                        Text(
                            text = downloadStatusLabel(download),
                            color = download.status.statusColor,
                            fontSize = 9.5.sp,
                        )
                    }
                    when (download.status) {
                        BrowserDownloadStatus.SUCCESSFUL -> {
                            PanelMiniAction("Open") { actions.openBrowserDownload(download.id) }
                            PanelMiniAction("Share") { actions.shareBrowserDownload(download.id) }
                        }

                        BrowserDownloadStatus.PENDING,
                        BrowserDownloadStatus.RUNNING,
                        BrowserDownloadStatus.PAUSED,
                        -> PanelMiniAction("Cancel") { actions.cancelBrowserDownload(download.id) }

                        BrowserDownloadStatus.FAILED,
                        BrowserDownloadStatus.CANCELLED,
                        -> Unit
                    }
                }
                if (download.totalBytes > 0 && download.status.isInProgress) {
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(
                                    (download.bytesDownloaded.toFloat() / download.totalBytes)
                                        .coerceIn(0f, 1f),
                                )
                                .fillMaxHeight()
                                .background(StagePalette.VioletBright),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowserLibraryRow(
    title: String,
    subtitle: String,
    onOpen: () -> Unit,
    trailingLabel: String? = null,
    onTrailing: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.025f))
            .clickable(onClick = onOpen)
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = StagePalette.TextPrimary, fontSize = 11.5.sp, maxLines = 1)
            Text(subtitle, color = StagePalette.TextTertiary, fontSize = 9.5.sp, maxLines = 1)
        }
        if (trailingLabel != null && onTrailing != null) {
            PanelMiniAction(trailingLabel, onTrailing)
        }
    }
}

@Composable
private fun PanelAction(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = StagePalette.TextPrimary, fontSize = 11.5.sp)
            Text(subtitle, color = StagePalette.TextTertiary, fontSize = 9.sp, maxLines = 1)
        }
        Text("›", color = StagePalette.TextTertiary, fontSize = 16.sp)
    }
}

@Composable
private fun PanelMiniAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        color = StagePalette.VioletBright,
        fontSize = 9.5.sp,
    )
}

@Composable
private fun PanelDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .height(1.dp)
            .background(StagePalette.Hairline),
    )
}

@Composable
private fun EmptyPanel(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, color = StagePalette.TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = StagePalette.TextSecondary, fontSize = 10.sp, lineHeight = 14.sp)
    }
}

@Composable
private fun BrowserErrorCard(
    message: String,
    onReload: () -> Unit,
    onOpenExternally: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .padding(18.dp)
            .widthIn(max = 330.dp)
            .border(1.dp, StagePalette.Hairline, RoundedCornerShape(16.dp)),
        color = Color(0xF51A1B21),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(17.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Page unavailable", color = StagePalette.TextPrimary, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Text(message, color = StagePalette.TextSecondary, fontSize = 10.5.sp, lineHeight = 15.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogButton("Open externally", onOpenExternally)
                DialogButton("Try again", onReload, highlighted = true)
            }
        }
    }
}

@Composable
private fun BrowserPreviewPage(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(listOf(Color(0xFF10141B), Color(0xFF0D0F14))),
            )
            .padding(26.dp),
    ) {
        Text("ANDROID DEVELOPERS", color = Color(0xFF7FD9FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Text(
            "Build for every\nAndroid screen",
            color = Color.White,
            fontSize = 25.sp,
            lineHeight = 29.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(9.dp))
        Text(
            "A live WebView replaces this deterministic screenshot surface on the device.",
            color = StagePalette.TextSecondary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            repeat(3) { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(
                            if (index == 0) Color(0xFF183449) else Color.White.copy(alpha = 0.045f),
                        )
                        .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(13.dp)),
                )
            }
        }
    }
}

@Composable
private fun ExternalLinkDialog(
    url: String,
    onCancel: () -> Unit,
    onOpen: () -> Unit,
) {
    val scheme = remember(url) { runCatching { url.toUri().scheme }.getOrNull() ?: "external" }
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .width(330.dp)
                .border(1.dp, StagePalette.Hairline, RoundedCornerShape(18.dp)),
            color = StagePalette.Window,
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Open another app?", color = StagePalette.TextPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    "This $scheme link will leave Stage Browser. Android will choose the receiving app.",
                    color = StagePalette.TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    url,
                    color = StagePalette.TextTertiary,
                    fontSize = 9.5.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DialogButton("Cancel", onCancel)
                    Spacer(Modifier.width(8.dp))
                    DialogButton("Open app", onOpen, highlighted = true)
                }
            }
        }
    }
}

@Composable
private fun DialogButton(
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (highlighted) StagePalette.Violet.copy(alpha = 0.26f)
                else Color.White.copy(alpha = 0.04f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        color = StagePalette.TextPrimary,
        fontSize = 10.5.sp,
    )
}

private fun panelTitle(panel: BrowserPanel): String = when (panel) {
    BrowserPanel.NONE -> "Browse"
    BrowserPanel.TABS -> "Tabs"
    BrowserPanel.MENU -> "Browser"
    BrowserPanel.PROFILES -> "Site Profile"
    BrowserPanel.BOOKMARKS -> "Bookmarks"
    BrowserPanel.HISTORY -> "History"
    BrowserPanel.DOWNLOADS -> "Downloads"
}

private fun hostLabel(url: String): String = runCatching { url.toUri().host }.getOrNull() ?: "this tab"

private fun formatTime(epochMillis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(epochMillis))

private val BrowserDownload.isInProgress: Boolean
    get() = status.isInProgress

private val BrowserDownloadStatus.isInProgress: Boolean
    get() = this == BrowserDownloadStatus.PENDING ||
        this == BrowserDownloadStatus.RUNNING ||
        this == BrowserDownloadStatus.PAUSED

private val BrowserDownloadStatus.statusColor: Color
    get() = when (this) {
        BrowserDownloadStatus.SUCCESSFUL -> StagePalette.Success
        BrowserDownloadStatus.FAILED -> StagePalette.Danger
        BrowserDownloadStatus.CANCELLED -> StagePalette.TextTertiary
        else -> StagePalette.VioletBright
    }

private fun downloadStatusLabel(download: BrowserDownload): String = when (download.status) {
    BrowserDownloadStatus.PENDING -> "Waiting for Android DownloadManager"
    BrowserDownloadStatus.RUNNING -> {
        if (download.totalBytes > 0) {
            "${formatBytes(download.bytesDownloaded)} of ${formatBytes(download.totalBytes)}"
        } else {
            "Downloading ${formatBytes(download.bytesDownloaded)}"
        }
    }

    BrowserDownloadStatus.PAUSED -> "Paused by Android"
    BrowserDownloadStatus.SUCCESSFUL -> "Saved in Downloads"
    BrowserDownloadStatus.FAILED -> "Failed${download.failureReason?.let { " · code $it" }.orEmpty()}"
    BrowserDownloadStatus.CANCELLED -> "Cancelled"
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 0 -> "Unknown size"
    bytes < 1_024 -> "$bytes B"
    bytes < 1_048_576 -> "${bytes / 1_024} KB"
    else -> String.format(Locale.getDefault(), "%.1f MB", bytes / 1_048_576.0)
}
