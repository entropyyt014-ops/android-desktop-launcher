package com.entropy.stage.shell

import com.entropy.stage.device.DeviceProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class StageShellModelTest {
    private val apps = listOf(
        app("Chrome", "com.android.chrome", "chrome"),
        app("Termux", "com.termux", "termux"),
        app("Stage Notes", "com.entropy.notes", "notes"),
    )

    @Test
    fun searchMatchesLabelsAndPackageNames() {
        assertEquals(listOf("termux"), filterInstalledApps(apps, "TERM").map { it.id })
        assertEquals(listOf("notes"), filterInstalledApps(apps, "entropy").map { it.id })
    }

    @Test
    fun blankSearchPreservesLauncherOrder() {
        assertEquals(apps, filterInstalledApps(apps, "  "))
    }

    @Test
    fun pinnedAppsFollowPersistedDockOrderAndIgnoreRemovedPackages() {
        val state = StageShellUiState(
            deviceProfile = DeviceProfile.preview(),
            apps = apps,
            pinnedAppIds = linkedSetOf("termux", "missing", "chrome"),
        )

        assertEquals(listOf("termux", "chrome"), state.pinnedApps.map { it.id })
    }

    @Test
    fun minimizeRestoreAndCloseKeepWindowStatesDistinct() {
        val opened = StageShellUiState(
            deviceProfile = DeviceProfile.preview(),
            activeSurface = StageSurface.BROWSER,
            windowMode = StageWindowMode.MAXIMIZED,
        )

        val minimized = opened.withMinimizedSurface()
        assertEquals(StageSurface.NONE, minimized.activeSurface)
        assertEquals(StageSurface.BROWSER, minimized.minimizedSurface)
        assertEquals(StageWindowMode.MAXIMIZED, minimized.windowMode)
        assertEquals(true, minimized.isSurfaceRunning(StageSurface.BROWSER))

        val restored = minimized.withOpenedSurface(StageSurface.BROWSER)
        assertEquals(StageSurface.BROWSER, restored.activeSurface)
        assertEquals(StageSurface.NONE, restored.minimizedSurface)
        assertEquals(StageWindowMode.MAXIMIZED, restored.windowMode)

        val closed = restored.withClosedSurface()
        assertEquals(StageSurface.NONE, closed.activeSurface)
        assertEquals(StageSurface.NONE, closed.minimizedSurface)
        assertEquals(StageWindowMode.FLOATING, closed.windowMode)
    }

    @Test
    fun openingAnotherSurfaceStartsItFloating() {
        val browser = StageShellUiState(
            deviceProfile = DeviceProfile.preview(),
            activeSurface = StageSurface.BROWSER,
            windowMode = StageWindowMode.MAXIMIZED,
        )

        val settings = browser.withOpenedSurface(StageSurface.SETTINGS)

        assertEquals(StageSurface.SETTINGS, settings.activeSurface)
        assertEquals(StageWindowMode.FLOATING, settings.windowMode)
    }

    private fun app(label: String, packageName: String, id: String): InstalledApp =
        InstalledApp(
            id = id,
            label = label,
            packageName = packageName,
            activityName = "MainActivity",
        )
}
