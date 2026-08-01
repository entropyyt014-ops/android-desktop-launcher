package com.entropy.stage

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import com.entropy.stage.designsystem.StageTheme
import com.entropy.stage.device.DeviceProfile
import com.entropy.stage.onboarding.StageOnboarding
import com.entropy.stage.shell.InstalledApp
import com.entropy.stage.shell.StageDesktop
import com.entropy.stage.shell.StageShellActions
import com.entropy.stage.shell.StageShellUiState
import com.entropy.stage.shell.StageSurface
import com.entropy.stage.shell.SystemDestination
import com.entropy.stage.shell.SystemStatus

@PreviewTest
@Preview(
    name = "Galaxy A30 Stage desktop",
    widthDp = 360,
    heightDp = 740,
    showBackground = true,
)
@Composable
fun stageDesktopPortraitScreenshot() {
    StageTheme {
        StageDesktop(
            state = previewState(),
            actions = PreviewActions,
            onRequestHomeRole = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "Landscape Applications window",
    widthDp = 740,
    heightDp = 360,
    showBackground = true,
)
@Composable
fun stageApplicationsLandscapeScreenshot() {
    StageTheme {
        StageDesktop(
            state = previewState(
                widthDp = 740,
                heightDp = 360,
                surface = StageSurface.APP_LIBRARY,
            ),
            actions = PreviewActions,
            onRequestHomeRole = {},
        )
    }
}

@PreviewTest
@Preview(
    name = "First run setup assistant",
    widthDp = 360,
    heightDp = 740,
    showBackground = true,
)
@Composable
fun stageOnboardingScreenshot() {
    StageTheme {
        StageOnboarding(
            profile = DeviceProfile.preview(),
            homeRoleHeld = false,
            homeRoleAvailable = true,
            reduceMotion = false,
            onRequestHomeRole = {},
            onFinish = {},
        )
    }
}

private fun previewState(
    widthDp: Int = 360,
    heightDp: Int = 740,
    surface: StageSurface = StageSurface.NONE,
): StageShellUiState {
    val apps = listOf(
        previewApp("Chrome", "com.android.chrome", "Main", "chrome"),
        previewApp("Camera", "com.android.camera", "Camera", "camera"),
        previewApp("Gallery", "com.samsung.gallery", "Gallery", "gallery"),
        previewApp("Messages", "com.android.messages", "Messages", "messages"),
        previewApp("YouTube", "com.google.youtube", "Home", "youtube"),
        previewApp("Termux", "com.termux", "TermuxActivity", "termux"),
        previewApp("Calculator", "com.android.calculator", "Calculator", "calculator"),
        previewApp("Clock", "com.android.clock", "Clock", "clock"),
    )
    return StageShellUiState(
        deviceProfile = DeviceProfile.preview(
            widthDp = widthDp,
            heightDp = heightDp,
            hasMouse = widthDp > heightDp,
            hasPhysicalKeyboard = widthDp > heightDp,
        ),
        preferencesLoaded = true,
        onboardingComplete = true,
        homeRoleHeld = true,
        homeRoleAvailable = true,
        apps = apps,
        appsLoading = false,
        pinnedAppIds = linkedSetOf("chrome", "termux"),
        activeSurface = surface,
        systemStatus = SystemStatus(
            clockLabel = "Sat 1 Aug  20:30",
            batteryPercent = 82,
            isOnline = true,
        ),
    )
}

private fun previewApp(
    label: String,
    packageName: String,
    activityName: String,
    id: String,
): InstalledApp = InstalledApp(
    id = id,
    label = label,
    packageName = packageName,
    activityName = activityName,
)

private object PreviewActions : StageShellActions {
    override fun completeOnboarding() = Unit
    override fun openSurface(surface: StageSurface) = Unit
    override fun closeSurface() = Unit
    override fun setCommandCenterOpen(open: Boolean) = Unit
    override fun setControlCenterOpen(open: Boolean) = Unit
    override fun setSearchQuery(query: String) = Unit
    override fun launchApp(app: InstalledApp) = Unit
    override fun togglePinned(app: InstalledApp) = Unit
    override fun openAppInfo(app: InstalledApp) = Unit
    override fun requestUninstall(app: InstalledApp) = Unit
    override fun openSystemDestination(destination: SystemDestination) = Unit
    override fun setReduceMotion(enabled: Boolean) = Unit
    override fun setDesktopGrain(enabled: Boolean) = Unit
    override fun setDockMagnification(enabled: Boolean) = Unit
    override fun refreshPlatformState() = Unit
    override fun dismissMessage() = Unit
}
