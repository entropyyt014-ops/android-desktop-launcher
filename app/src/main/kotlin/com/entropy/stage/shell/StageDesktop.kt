package com.entropy.stage.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGestures
import com.entropy.stage.design.StageAppIcon
import com.entropy.stage.design.StageDockButton
import com.entropy.stage.design.StageGlyph
import com.entropy.stage.design.StageSymbol
import com.entropy.stage.design.StageSymbolType
import com.entropy.stage.design.WindowTrafficLights
import com.entropy.stage.browser.BrowserCommandType
import com.entropy.stage.browser.StageBrowserPanel
import com.entropy.stage.designsystem.StageDimensions
import com.entropy.stage.designsystem.StagePalette
import com.entropy.stage.designsystem.stageDimensionsFor
import com.entropy.stage.launcher.AppLibraryPanel
import com.entropy.stage.settings.StageSettingsPanel
import kotlin.math.roundToInt

@Composable
fun StageDesktop(
    state: StageShellUiState,
    actions: StageShellActions,
    onRequestHomeRole: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val command = event.isCtrlPressed || event.isMetaPressed
                when {
                    command && event.key == Key.Spacebar -> {
                        actions.setCommandCenterOpen(!state.commandCenterOpen)
                        true
                    }

                    command && event.key == Key.Comma -> {
                        actions.openSurface(StageSurface.SETTINGS)
                        true
                    }

                    command && event.key == Key.L -> {
                        actions.openSurface(StageSurface.BROWSER)
                        actions.requestBrowserCommand(BrowserCommandType.FOCUS_ADDRESS)
                        true
                    }

                    command && event.key == Key.T && event.isShiftPressed -> {
                        actions.reopenClosedBrowserTab()
                        true
                    }

                    command && event.key == Key.T -> {
                        actions.newBrowserTab()
                        true
                    }

                    command && event.key == Key.W -> {
                        if (state.activeSurface == StageSurface.BROWSER) {
                            actions.closeBrowserTab(state.browser.activeTabId)
                        } else {
                            actions.closeSurface()
                        }
                        true
                    }

                    command && event.key == Key.F -> {
                        if (state.activeSurface == StageSurface.BROWSER) {
                            actions.setBrowserFindVisible(true)
                        } else {
                            actions.setCommandCenterOpen(true)
                        }
                        true
                    }

                    command && event.key == Key.Tab -> {
                        if (state.activeSurface == StageSurface.BROWSER) {
                            actions.cycleBrowserTab(forward = !event.isShiftPressed)
                            true
                        } else {
                            false
                        }
                    }

                    event.isAltPressed && event.key == Key.DirectionLeft &&
                        state.activeSurface == StageSurface.BROWSER -> {
                        actions.requestBrowserCommand(BrowserCommandType.BACK)
                        true
                    }

                    event.isAltPressed && event.key == Key.DirectionRight &&
                        state.activeSurface == StageSurface.BROWSER -> {
                        actions.requestBrowserCommand(BrowserCommandType.FORWARD)
                        true
                    }

                    event.isAltPressed && event.key == Key.Tab -> {
                        actions.openSurface(
                            when (state.activeSurface) {
                                StageSurface.BROWSER -> StageSurface.APP_LIBRARY
                                StageSurface.APP_LIBRARY -> StageSurface.SETTINGS
                                else -> StageSurface.BROWSER
                            },
                        )
                        true
                    }

                    event.key == Key.Escape -> {
                        when {
                            state.commandCenterOpen -> actions.setCommandCenterOpen(false)
                            state.controlCenterOpen -> actions.setControlCenterOpen(false)
                            state.activeSurface != StageSurface.NONE -> actions.closeSurface()
                            else -> return@onPreviewKeyEvent false
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusable()
            .background(StagePalette.Ink)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        val dimensions = stageDimensionsFor(maxWidth, state.deviceProfile.fontScale)
        val floatingWindows = maxWidth > maxHeight && maxWidth >= 600.dp

        StageWallpaper(
            showGrain = state.desktopGrain,
            modifier = Modifier.fillMaxSize(),
        )

        DesktopShortcuts(
            homeRoleHeld = state.homeRoleHeld,
            onOpenApps = { actions.openSurface(StageSurface.APP_LIBRARY) },
            onRequestHome = onRequestHomeRole,
            modifier = if (floatingWindows) {
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 22.dp, top = dimensions.topStripHeight + 20.dp)
            } else {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 14.dp, top = dimensions.topStripHeight + 20.dp)
            },
        )

        StageWindowHost(
            state = state,
            actions = actions,
            onRequestHomeRole = onRequestHomeRole,
            floating = floatingWindows,
            dimensions = dimensions,
            availableWidth = maxWidth,
            availableHeight = maxHeight,
        )

        StageMenuBar(
            state = state,
            actions = actions,
            expandedMenus = floatingWindows,
            dimensions = dimensions,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        StageDock(
            state = state,
            actions = actions,
            dimensions = dimensions,
            maxAvailableWidth = maxWidth - 16.dp,
            maxAvailableHeight = maxHeight - dimensions.topStripHeight - 18.dp,
            vertical = floatingWindows,
            modifier = if (floatingWindows) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 9.dp, top = dimensions.topStripHeight + 8.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 9.dp)
            },
        )

        if (state.commandCenterOpen) {
            CommandCenterOverlay(
                state = state,
                actions = actions,
                onDismiss = { actions.setCommandCenterOpen(false) },
            )
        }

        if (state.controlCenterOpen) {
            StageControlCenter(
                state = state,
                actions = actions,
                onRequestHomeRole = onRequestHomeRole,
                onDismiss = { actions.setControlCenterOpen(false) },
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun StageWallpaper(
    showGrain: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            Brush.linearGradient(
                colors = listOf(Color(0xFF090A0F), Color(0xFF17131F), Color(0xFF090B11)),
                start = Offset.Zero,
                end = Offset(size.width, size.height),
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7447CE).copy(alpha = 0.32f), Color.Transparent),
                center = Offset(size.width * 0.18f, size.height * 0.18f),
                radius = size.minDimension * 0.68f,
            ),
            radius = size.minDimension * 0.68f,
            center = Offset(size.width * 0.18f, size.height * 0.18f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF183D6F).copy(alpha = 0.36f), Color.Transparent),
                center = Offset(size.width * 0.86f, size.height * 0.72f),
                radius = size.minDimension * 0.76f,
            ),
            radius = size.minDimension * 0.76f,
            center = Offset(size.width * 0.86f, size.height * 0.72f),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFB279FF).copy(alpha = 0.08f), Color.Transparent),
                center = Offset(size.width * 0.52f, size.height * 0.48f),
                radius = size.maxDimension * 0.42f,
            ),
            radius = size.maxDimension * 0.42f,
            center = Offset(size.width * 0.52f, size.height * 0.48f),
        )
        if (showGrain) {
            repeat(210) { index ->
                val x = ((index * 97 + 31) % 997) / 997f * size.width
                val y = ((index * 53 + 17) % 991) / 991f * size.height
                val alpha = if (index % 3 == 0) 0.034f else 0.018f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = if (index % 5 == 0) 0.9f else 0.55f,
                    center = Offset(x, y),
                )
            }
        }
    }
}

@Composable
private fun StageMenuBar(
    state: StageShellUiState,
    actions: StageShellActions,
    expandedMenus: Boolean,
    dimensions: StageDimensions,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensions.topStripHeight)
            .background(Color(0xD817181E))
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.1f))
            .padding(horizontal = if (expandedMenus) 13.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { actions.openSurface(StageSurface.SETTINGS) }
                .padding(7.dp),
        ) {
            StageGlyph(Modifier.fillMaxSize())
        }
        Text(
            text = activeTitle(state.activeSurface),
            modifier = Modifier.padding(start = 3.dp),
            color = StagePalette.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (expandedMenus) {
            Spacer(Modifier.width(12.dp))
            MenuAction("File") {
                if (state.activeSurface == StageSurface.BROWSER) actions.newBrowserTab()
                else actions.openSurface(StageSurface.APP_LIBRARY)
            }
            MenuAction("View") {
                if (state.activeSurface == StageSurface.BROWSER) {
                    actions.setBrowserFocusMode(!state.browser.focusMode)
                } else {
                    actions.setCommandCenterOpen(true)
                }
            }
            MenuAction("Window") {
                if (state.activeSurface == StageSurface.NONE) {
                    actions.openSurface(StageSurface.APP_LIBRARY)
                } else {
                    actions.closeSurface()
                }
            }
            MenuAction("Help") { actions.openSurface(StageSurface.SETTINGS) }
        }
        Spacer(Modifier.weight(1f))
        if (state.systemStatus.isOnline) {
            StageSymbol(
                type = StageSymbolType.WIFI,
                modifier = Modifier.size(15.dp),
                tint = StagePalette.TextSecondary,
            )
            Spacer(Modifier.width(7.dp))
        }
        BatteryIndicator(percent = state.systemStatus.batteryPercent)
        Spacer(Modifier.width(8.dp))
        Text(
            text = state.systemStatus.clockLabel,
            color = StagePalette.TextPrimary,
            fontSize = if (expandedMenus) 10.5.sp else 9.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .padding(start = 5.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { actions.setControlCenterOpen(!state.controlCenterOpen) }
                .padding(7.dp),
        ) {
            StageSymbol(
                type = StageSymbolType.CONTROL,
                modifier = Modifier.fillMaxSize(),
                tint = if (state.controlCenterOpen) StagePalette.VioletBright
                else StagePalette.TextPrimary,
            )
        }
    }
}

@Composable
private fun MenuAction(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        color = StagePalette.TextPrimary,
        fontSize = 11.sp,
    )
}

@Composable
private fun BatteryIndicator(percent: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(10.dp)
                .border(1.dp, StagePalette.TextSecondary, RoundedCornerShape(3.dp))
                .padding(1.5.dp),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((percent.coerceIn(0, 100) / 100f).coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (percent <= 15) StagePalette.Danger else StagePalette.TextPrimary),
            )
        }
        Box(
            Modifier
                .width(2.dp)
                .height(5.dp)
                .background(StagePalette.TextSecondary, RoundedCornerShape(1.dp)),
        )
    }
}

@Composable
private fun DesktopShortcuts(
    homeRoleHeld: Boolean,
    onOpenApps: () -> Unit,
    onRequestHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        DesktopShortcut(
            label = "Applications",
            symbol = StageSymbolType.FOLDER,
            onClick = onOpenApps,
        )
        if (!homeRoleHeld) {
            DesktopShortcut(
                label = "Make Stage Home",
                symbol = StageSymbolType.WINDOW,
                onClick = onRequestHome,
            )
        }
    }
}

@Composable
private fun DesktopShortcut(
    label: String,
    symbol: StageSymbolType,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(78.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .shadow(10.dp, RoundedCornerShape(13.dp))
                .clip(RoundedCornerShape(13.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFFB79CFF), Color(0xFF7049BA)),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(13.dp))
                .padding(9.dp),
        ) {
            StageSymbol(symbol, Modifier.fillMaxSize(), Color.White)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            color = Color.White,
            fontSize = 9.5.sp,
            lineHeight = 11.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = Color.Black.copy(alpha = 0.92f),
                    offset = Offset(0f, 2f),
                    blurRadius = 5f,
                ),
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun StageDock(
    state: StageShellUiState,
    actions: StageShellActions,
    dimensions: StageDimensions,
    maxAvailableWidth: androidx.compose.ui.unit.Dp,
    maxAvailableHeight: androidx.compose.ui.unit.Dp,
    vertical: Boolean,
    modifier: Modifier = Modifier,
) {
    val surfaceModifier = if (vertical) {
        modifier
            .width(dimensions.dockHeight)
            .heightIn(max = maxAvailableHeight)
    } else {
        modifier
            .widthIn(max = maxAvailableWidth)
            .height(dimensions.dockHeight)
    }

    Surface(
        modifier = surfaceModifier
            .shadow(
                24.dp,
                RoundedCornerShape(24.dp),
                ambientColor = Color.Black.copy(alpha = 0.7f),
                spotColor = Color.Black.copy(alpha = 0.9f),
            )
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(24.dp)),
        color = Color(0xD91B1B22),
        shape = RoundedCornerShape(24.dp),
    ) {
        if (vertical) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 7.dp, vertical = 9.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StageDockItems(state = state, actions = actions, vertical = true)
            }
        } else {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StageDockItems(state = state, actions = actions, vertical = false)
            }
        }
    }
}

@Composable
private fun StageDockItems(
    state: StageShellUiState,
    actions: StageShellActions,
    vertical: Boolean,
) {
    StageDockButton(
        label = "Applications",
        selected = state.activeSurface == StageSurface.APP_LIBRARY,
        magnification = state.dockMagnification && !state.reduceMotion,
        onClick = { actions.openSurface(StageSurface.APP_LIBRARY) },
    ) {
        StageSymbol(StageSymbolType.APPS, Modifier.fillMaxSize())
    }
    StageDockButton(
        label = "Browse",
        selected = state.activeSurface == StageSurface.BROWSER,
        magnification = state.dockMagnification && !state.reduceMotion,
        onClick = { actions.openSurface(StageSurface.BROWSER) },
    ) {
        StageSymbol(StageSymbolType.BROWSER, Modifier.fillMaxSize())
    }
    StageDockButton(
        label = "Command Center",
        selected = state.commandCenterOpen,
        magnification = state.dockMagnification && !state.reduceMotion,
        onClick = { actions.setCommandCenterOpen(!state.commandCenterOpen) },
    ) {
        StageSymbol(StageSymbolType.SEARCH, Modifier.fillMaxSize())
    }
    StageDockButton(
        label = "Settings",
        selected = state.activeSurface == StageSurface.SETTINGS,
        magnification = state.dockMagnification && !state.reduceMotion,
        onClick = { actions.openSurface(StageSurface.SETTINGS) },
    ) {
        StageSymbol(StageSymbolType.SETTINGS, Modifier.fillMaxSize())
    }
    if (state.pinnedApps.isNotEmpty()) {
        Box(
            if (vertical) {
                Modifier
                    .padding(vertical = 4.dp)
                    .width(38.dp)
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.14f))
            } else {
                Modifier
                    .padding(horizontal = 4.dp)
                    .width(1.dp)
                    .height(38.dp)
                    .background(Color.White.copy(alpha = 0.14f))
            },
        )
        state.pinnedApps.forEach { app ->
            StageDockButton(
                label = app.label,
                selected = false,
                magnification = state.dockMagnification && !state.reduceMotion,
                onClick = { actions.launchApp(app) },
            ) {
                StageAppIcon(app, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun StageWindowHost(
    state: StageShellUiState,
    actions: StageShellActions,
    onRequestHomeRole: () -> Unit,
    floating: Boolean,
    dimensions: StageDimensions,
    availableWidth: androidx.compose.ui.unit.Dp,
    availableHeight: androidx.compose.ui.unit.Dp,
) {
    val visible = state.activeSurface != StageSurface.NONE
    val transitionDuration = if (state.reduceMotion) 0 else 210
    var maximized by remember(state.activeSurface, floating) { mutableStateOf(false) }
    var dragX by remember(state.activeSurface) { mutableFloatStateOf(0f) }
    var dragY by remember(state.activeSurface) { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val maxDragX = with(density) { (availableWidth * 0.1f).toPx() }
    val maxDragY = with(density) { (availableHeight * 0.1f).toPx() }

    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(transitionDuration)) + scaleIn(
            animationSpec = tween(transitionDuration),
            initialScale = 0.96f,
        ),
        exit = fadeOut(tween(transitionDuration)) + scaleOut(
            animationSpec = tween(transitionDuration),
            targetScale = 0.96f,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (floating && !maximized) Alignment.TopCenter else Alignment.Center,
        ) {
            val windowModifier = if (floating && !maximized) {
                Modifier
                    .width(availableWidth * 0.78f)
                    .height(availableHeight - dimensions.topStripHeight - 16.dp)
                    .offset(y = dimensions.topStripHeight + 8.dp)
                    .offset { IntOffset(dragX.roundToInt(), dragY.roundToInt()) }
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (floating) 8.dp else 7.dp,
                        end = if (floating) 8.dp else 7.dp,
                        top = dimensions.topStripHeight + 8.dp,
                        bottom = dimensions.dockHeight + 18.dp,
                    )
            }

            Surface(
                modifier = windowModifier
                    .shadow(
                        36.dp,
                        RoundedCornerShape(dimensions.windowRadius),
                        ambientColor = Color.Black.copy(alpha = 0.65f),
                        spotColor = Color.Black.copy(alpha = 0.9f),
                    )
                    .border(
                        1.dp,
                        Color.White.copy(alpha = 0.14f),
                        RoundedCornerShape(dimensions.windowRadius),
                    ),
                color = StagePalette.Window,
                shape = RoundedCornerShape(dimensions.windowRadius),
            ) {
                Column(Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .background(Color.White.copy(alpha = 0.025f))
                            .then(
                                if (floating && !maximized) {
                                    Modifier.pointerInput(state.activeSurface) {
                                        detectDragGestures { change, dragAmount ->
                                            change.consume()
                                            dragX = (dragX + dragAmount.x).coerceIn(-maxDragX, maxDragX)
                                            dragY = (dragY + dragAmount.y).coerceIn(-maxDragY, maxDragY)
                                        }
                                    }
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        WindowTrafficLights(
                            onClose = actions::closeSurface,
                            onMinimize = actions::closeSurface,
                            onMaximize = { if (floating) maximized = !maximized },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 14.dp),
                        )
                        Text(
                            text = activeTitle(state.activeSurface),
                            modifier = Modifier.align(Alignment.Center),
                            color = StagePalette.TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(StagePalette.Hairline),
                    )
                    when (state.activeSurface) {
                        StageSurface.BROWSER -> StageBrowserPanel(
                            state = state.browser,
                            actions = actions,
                            hasPhysicalKeyboard = state.deviceProfile.hasPhysicalKeyboard,
                            modifier = Modifier.fillMaxSize(),
                        )

                        StageSurface.APP_LIBRARY -> AppLibraryPanel(
                            state = state,
                            actions = actions,
                            compactLayout = floating,
                            modifier = Modifier.fillMaxSize(),
                        )

                        StageSurface.SETTINGS -> StageSettingsPanel(
                            state = state,
                            actions = actions,
                            onRequestHomeRole = onRequestHomeRole,
                            modifier = Modifier.fillMaxSize(),
                        )

                        StageSurface.NONE -> Unit
                    }
                }
            }
        }
    }
}

private fun activeTitle(surface: StageSurface): String = when (surface) {
    StageSurface.NONE -> "Stage"
    StageSurface.BROWSER -> "Browse"
    StageSurface.APP_LIBRARY -> "Applications"
    StageSurface.SETTINGS -> "Settings"
}
