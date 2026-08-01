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

    private fun app(label: String, packageName: String, id: String): InstalledApp =
        InstalledApp(
            id = id,
            label = label,
            packageName = packageName,
            activityName = "MainActivity",
        )
}
