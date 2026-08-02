package com.entropy.stage.shell

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.entropy.stage.browser.BrowserActions
import com.entropy.stage.browser.BrowserUiState
import com.entropy.stage.device.DeviceProfile

enum class StageSurface {
    NONE,
    BROWSER,
    APP_LIBRARY,
    SETTINGS,
}

enum class StageWindowMode {
    FLOATING,
    MAXIMIZED,
}

enum class SystemDestination {
    HOME,
    WIFI,
    BLUETOOTH,
    DISPLAY,
    SOUND,
    APP_DETAILS,
}

data class InstalledApp(
    val id: String,
    val label: String,
    val packageName: String,
    val activityName: String,
    val userHandle: UserHandle? = null,
    val icon: Drawable? = null,
    val isSystemApp: Boolean = false,
) {
    val componentName: ComponentName
        get() = ComponentName(packageName, activityName)
}

data class SystemStatus(
    val clockLabel: String = "Sat 1 Aug  20:30",
    val batteryPercent: Int = 100,
    val isOnline: Boolean = true,
)

data class StagePreferencesState(
    val onboardingComplete: Boolean = false,
    val pinnedAppIds: Set<String> = emptySet(),
    val lastSurface: StageSurface = StageSurface.NONE,
    val reduceMotion: Boolean = false,
    val desktopGrain: Boolean = true,
    val dockMagnification: Boolean = true,
)

data class StageShellUiState(
    val deviceProfile: DeviceProfile,
    val preferencesLoaded: Boolean = false,
    val onboardingComplete: Boolean = false,
    val homeRoleHeld: Boolean = false,
    val homeRoleAvailable: Boolean = true,
    val apps: List<InstalledApp> = emptyList(),
    val appsLoading: Boolean = true,
    val pinnedAppIds: Set<String> = emptySet(),
    val activeSurface: StageSurface = StageSurface.NONE,
    val minimizedSurface: StageSurface = StageSurface.NONE,
    val windowMode: StageWindowMode = StageWindowMode.FLOATING,
    val commandCenterOpen: Boolean = false,
    val controlCenterOpen: Boolean = false,
    val searchQuery: String = "",
    val systemStatus: SystemStatus = SystemStatus(),
    val reduceMotion: Boolean = false,
    val desktopGrain: Boolean = true,
    val dockMagnification: Boolean = true,
    val browser: BrowserUiState = BrowserUiState(),
    val message: String? = null,
) {
    val pinnedApps: List<InstalledApp>
        get() = pinnedAppIds.mapNotNull { id -> apps.firstOrNull { it.id == id } }

    fun isSurfaceRunning(surface: StageSurface): Boolean =
        surface != StageSurface.NONE &&
            (activeSurface == surface || minimizedSurface == surface)
}

interface StageShellActions : BrowserActions {
    fun completeOnboarding()
    fun openSurface(surface: StageSurface)
    fun closeSurface()
    fun minimizeSurface()
    fun toggleMaximizeSurface()
    fun setCommandCenterOpen(open: Boolean)
    fun setControlCenterOpen(open: Boolean)
    fun setSearchQuery(query: String)
    fun launchApp(app: InstalledApp)
    fun togglePinned(app: InstalledApp)
    fun openAppInfo(app: InstalledApp)
    fun requestUninstall(app: InstalledApp)
    fun openSystemDestination(destination: SystemDestination)
    fun setReduceMotion(enabled: Boolean)
    fun setDesktopGrain(enabled: Boolean)
    fun setDockMagnification(enabled: Boolean)
    fun refreshPlatformState()
    fun dismissMessage()
}

internal fun StageShellUiState.withOpenedSurface(surface: StageSurface): StageShellUiState {
    if (surface == StageSurface.NONE) return withClosedSurface()
    val restoreMinimized = minimizedSurface == surface
    return copy(
        activeSurface = surface,
        minimizedSurface = StageSurface.NONE,
        windowMode = if (restoreMinimized || activeSurface == surface) {
            windowMode
        } else {
            StageWindowMode.FLOATING
        },
        commandCenterOpen = false,
        controlCenterOpen = false,
    )
}

internal fun StageShellUiState.withClosedSurface(): StageShellUiState = copy(
    activeSurface = StageSurface.NONE,
    minimizedSurface = StageSurface.NONE,
    windowMode = StageWindowMode.FLOATING,
)

internal fun StageShellUiState.withMinimizedSurface(): StageShellUiState {
    if (activeSurface == StageSurface.NONE) return this
    return copy(
        activeSurface = StageSurface.NONE,
        minimizedSurface = activeSurface,
        commandCenterOpen = false,
        controlCenterOpen = false,
    )
}

internal fun StageShellUiState.withToggledMaximize(): StageShellUiState {
    if (activeSurface == StageSurface.NONE) return this
    return copy(
        windowMode = when (windowMode) {
            StageWindowMode.FLOATING -> StageWindowMode.MAXIMIZED
            StageWindowMode.MAXIMIZED -> StageWindowMode.FLOATING
        },
    )
}

fun filterInstalledApps(
    apps: List<InstalledApp>,
    query: String,
): List<InstalledApp> {
    val normalized = query.trim()
    if (normalized.isEmpty()) return apps
    return apps.filter { app ->
        app.label.contains(normalized, ignoreCase = true) ||
            app.packageName.contains(normalized, ignoreCase = true)
    }
}
