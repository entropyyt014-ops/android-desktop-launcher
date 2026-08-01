package com.entropy.stage.shell

import android.content.ComponentName
import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.entropy.stage.device.DeviceProfile

enum class StageSurface {
    NONE,
    APP_LIBRARY,
    SETTINGS,
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
    val commandCenterOpen: Boolean = false,
    val controlCenterOpen: Boolean = false,
    val searchQuery: String = "",
    val systemStatus: SystemStatus = SystemStatus(),
    val reduceMotion: Boolean = false,
    val desktopGrain: Boolean = true,
    val dockMagnification: Boolean = true,
    val message: String? = null,
) {
    val pinnedApps: List<InstalledApp>
        get() = pinnedAppIds.mapNotNull { id -> apps.firstOrNull { it.id == id } }
}

interface StageShellActions {
    fun completeOnboarding()
    fun openSurface(surface: StageSurface)
    fun closeSurface()
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
