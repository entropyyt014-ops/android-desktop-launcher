package com.entropy.stage.launcher

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.os.UserHandle
import android.provider.Settings
import com.entropy.stage.shell.InstalledApp
import java.text.Collator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InstalledAppsRepository(
    private val context: Context,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    private val _loading = MutableStateFlow(true)
    private var scope: CoroutineScope? = null

    val apps: StateFlow<List<InstalledApp>> = _apps
    val loading: StateFlow<Boolean> = _loading

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh()
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh()
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh()

        override fun onPackagesAvailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()

        override fun onPackagesUnavailable(
            packageNames: Array<out String>,
            user: UserHandle,
            replacing: Boolean,
        ) = refresh()
    }

    fun start(scope: CoroutineScope) {
        this.scope = scope
        runCatching { launcherApps.registerCallback(packageCallback) }
        refresh()
    }

    fun stop() {
        runCatching { launcherApps.unregisterCallback(packageCallback) }
        scope = null
    }

    fun refresh() {
        val activeScope = scope ?: return
        activeScope.launch {
            _loading.value = true
            _apps.value = queryApps()
            _loading.value = false
        }
    }

    fun launch(app: InstalledApp): Result<Unit> = runCatching {
        launcherApps.startMainActivity(
            app.componentName,
            app.userHandle ?: Process.myUserHandle(),
            null,
            null,
        )
    }

    fun openAppInfo(app: InstalledApp): Result<Unit> = runCatching {
        context.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${app.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun requestUninstall(app: InstalledApp): Result<Unit> = runCatching {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private suspend fun queryApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val density = context.resources.displayMetrics.densityDpi
        val profiles = launcherApps.profiles.ifEmpty { listOf(Process.myUserHandle()) }
        val collator = Collator.getInstance()

        profiles.flatMap { user ->
            launcherApps.getActivityList(null, user).mapNotNull { info ->
                val component = info.componentName
                if (component.packageName == context.packageName) return@mapNotNull null
                InstalledApp(
                    id = appId(component, user),
                    label = info.label?.toString()?.ifBlank { component.packageName }
                        ?: component.packageName,
                    packageName = component.packageName,
                    activityName = component.className,
                    userHandle = user,
                    icon = runCatching { info.getBadgedIcon(density) }.getOrNull(),
                    isSystemApp =
                        info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
        }.distinctBy(InstalledApp::id)
            .sortedWith { left, right -> collator.compare(left.label, right.label) }
    }

    private fun appId(componentName: ComponentName, user: UserHandle): String =
        "${componentName.flattenToShortString()}@${user.hashCode()}"
}
