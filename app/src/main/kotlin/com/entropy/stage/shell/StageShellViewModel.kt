package com.entropy.stage.shell

import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import com.entropy.stage.browser.BROWSER_HOME_URL
import com.entropy.stage.browser.BrowserBookmark
import com.entropy.stage.browser.BrowserCommand
import com.entropy.stage.browser.BrowserCommandType
import com.entropy.stage.browser.BrowserDownloadRequest
import com.entropy.stage.browser.BrowserDownloadStatus
import com.entropy.stage.browser.BrowserHistoryEntry
import com.entropy.stage.browser.BrowserPageSnapshot
import com.entropy.stage.browser.BrowserPanel
import com.entropy.stage.browser.BrowserPlatformController
import com.entropy.stage.browser.BrowserProfile
import com.entropy.stage.browser.BrowserSessionRepository
import com.entropy.stage.browser.BrowserTab
import com.entropy.stage.browser.BrowserUiState
import com.entropy.stage.browser.normalizeBrowserInput
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.entropy.stage.data.StagePreferencesRepository
import com.entropy.stage.device.DeviceProfile
import com.entropy.stage.launcher.InstalledAppsRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class StageShellViewModel(
    application: Application,
    initialProfile: DeviceProfile,
) : AndroidViewModel(application), StageShellActions {
    private val preferences = StagePreferencesRepository(application)
    private val installedApps = InstalledAppsRepository(application)
    private val browserSession = BrowserSessionRepository(application)
    private val browserPlatform = BrowserPlatformController(application)
    private val _uiState = MutableStateFlow(StageShellUiState(deviceProfile = initialProfile))
    private val browserSaveMutex = Mutex()
    private var restoredSession = false
    private var browserCommandSerial = 0L
    private var browserTabSerial = 1L

    val uiState: StateFlow<StageShellUiState> = _uiState.asStateFlow()

    init {
        installedApps.start(viewModelScope)

        viewModelScope.launch {
            val saved = browserSession.load()
            val activeTab = saved.tabs.firstOrNull { it.id == saved.activeTabId }
                ?: saved.tabs.first()
            val current = _uiState.value.browser
            _uiState.value = _uiState.value.copy(
                browser = current.copy(
                    tabs = saved.tabs,
                    activeTabId = activeTab.id,
                    closedTabs = saved.closedTabs,
                    addressInput = activeTab.url,
                    bookmarks = saved.bookmarks,
                    history = saved.history,
                    downloads = saved.downloads,
                    siteProfiles = saved.siteProfiles,
                    sessionLoaded = true,
                ),
            )
            refreshBrowserDownloads()
        }

        viewModelScope.launch {
            preferences.state.collect { saved ->
                val restoredSurface = if (restoredSession) {
                    _uiState.value.activeSurface
                } else {
                    saved.lastSurface
                }
                restoredSession = true
                _uiState.value = _uiState.value.copy(
                    preferencesLoaded = true,
                    onboardingComplete = saved.onboardingComplete,
                    pinnedAppIds = saved.pinnedAppIds,
                    activeSurface = restoredSurface,
                    reduceMotion = saved.reduceMotion,
                    desktopGrain = saved.desktopGrain,
                    dockMagnification = saved.dockMagnification,
                )
            }
        }
        viewModelScope.launch {
            installedApps.apps.collect { apps ->
                _uiState.value = _uiState.value.copy(apps = apps)
            }
        }
        viewModelScope.launch {
            installedApps.loading.collect { loading ->
                _uiState.value = _uiState.value.copy(appsLoading = loading)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                refreshStatusOnly()
                delay(30_000)
            }
        }
        refreshPlatformState()
    }

    fun updateDeviceProfile(profile: DeviceProfile) {
        _uiState.value = _uiState.value.copy(deviceProfile = profile)
    }

    override fun completeOnboarding() {
        viewModelScope.launch { preferences.setOnboardingComplete(true) }
    }

    override fun openSurface(surface: StageSurface) {
        _uiState.value = _uiState.value.copy(
            activeSurface = surface,
            commandCenterOpen = false,
            controlCenterOpen = false,
        )
        viewModelScope.launch { preferences.setLastSurface(surface) }
    }

    override fun closeSurface() {
        openSurface(StageSurface.NONE)
    }

    override fun setCommandCenterOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            commandCenterOpen = open,
            controlCenterOpen = if (open) false else _uiState.value.controlCenterOpen,
            searchQuery = if (open) _uiState.value.searchQuery else "",
        )
    }

    override fun setControlCenterOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(
            controlCenterOpen = open,
            commandCenterOpen = if (open) false else _uiState.value.commandCenterOpen,
        )
    }

    override fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    override fun launchApp(app: InstalledApp) {
        installedApps.launch(app).onSuccess {
            _uiState.value = _uiState.value.copy(
                commandCenterOpen = false,
                message = "Opening ${app.label}",
            )
        }.onFailure { failure ->
            showError("Could not open ${app.label}", failure)
        }
    }

    override fun togglePinned(app: InstalledApp) {
        viewModelScope.launch { preferences.togglePinned(app.id) }
    }

    override fun openAppInfo(app: InstalledApp) {
        installedApps.openAppInfo(app).onFailure { failure ->
            showError("App information is unavailable", failure)
        }
    }

    override fun requestUninstall(app: InstalledApp) {
        installedApps.requestUninstall(app).onFailure { failure ->
            showError("Android could not open uninstall", failure)
        }
    }

    override fun openSystemDestination(destination: SystemDestination) {
        val action = when (destination) {
            SystemDestination.HOME -> Settings.ACTION_HOME_SETTINGS
            SystemDestination.WIFI -> Settings.ACTION_WIFI_SETTINGS
            SystemDestination.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            SystemDestination.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            SystemDestination.SOUND -> Settings.ACTION_SOUND_SETTINGS
            SystemDestination.APP_DETAILS -> Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        }
        runCatching {
            getApplication<Application>().startActivity(
                Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure { failure -> showError("Android Settings could not open", failure) }
    }

    override fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceMotion(enabled) }
    }

    override fun setDesktopGrain(enabled: Boolean) {
        viewModelScope.launch { preferences.setDesktopGrain(enabled) }
    }

    override fun setDockMagnification(enabled: Boolean) {
        viewModelScope.launch { preferences.setDockMagnification(enabled) }
    }

    override fun newBrowserTab(url: String?) {
        val target = normalizeBrowserInput(url ?: BROWSER_HOME_URL)
        openSurface(StageSurface.BROWSER)
        updateBrowser(persist = true) { browser ->
            val id = nextBrowserTabId()
            val profile = profileForUrl(browser, target)
            val tab = BrowserTab(id = id, url = target, profile = profile, isLoading = true)
            browser.copy(
                tabs = (browser.tabs.takeLast(MAX_BROWSER_TABS - 1) + tab),
                activeTabId = id,
                addressInput = target,
                panel = BrowserPanel.NONE,
                focusMode = false,
                command = null,
            )
        }
    }

    override fun closeBrowserTab(tabId: String) {
        val current = _uiState.value.browser
        val closingIndex = current.tabs.indexOfFirst { it.id == tabId }
        val closing = current.tabs.getOrNull(closingIndex) ?: return
        if (current.tabs.size == 1) {
            val replacement = BrowserTab(id = nextBrowserTabId())
            updateBrowser(persist = true) { browser ->
                browser.copy(
                    tabs = listOf(replacement),
                    activeTabId = replacement.id,
                    addressInput = replacement.url,
                    closedTabs = (listOf(closing) + browser.closedTabs).take(MAX_CLOSED_TABS),
                    panel = BrowserPanel.NONE,
                    command = null,
                )
            }
            closeSurface()
            return
        }

        updateBrowser(persist = true) { browser ->
            val remaining = browser.tabs.filterNot { it.id == tabId }
            val nextIndex = closingIndex.coerceAtMost(remaining.lastIndex)
            val activeId = if (browser.activeTabId == tabId) {
                remaining[nextIndex].id
            } else {
                browser.activeTabId
            }
            val active = remaining.first { it.id == activeId }
            browser.copy(
                tabs = remaining,
                activeTabId = activeId,
                addressInput = active.url,
                closedTabs = (listOf(closing) + browser.closedTabs).take(MAX_CLOSED_TABS),
                panel = BrowserPanel.NONE,
                command = null,
            )
        }
    }

    override fun selectBrowserTab(tabId: String) {
        updateBrowser { browser ->
            val tab = browser.tabs.firstOrNull { it.id == tabId } ?: return@updateBrowser browser
            browser.copy(
                activeTabId = tab.id,
                addressInput = tab.url,
                panel = BrowserPanel.NONE,
                command = null,
            )
        }
        persistCurrentBrowser()
    }

    override fun cycleBrowserTab(forward: Boolean) {
        val browser = _uiState.value.browser
        if (browser.tabs.size < 2) return
        val currentIndex = browser.tabs.indexOfFirst { it.id == browser.activeTabId }.coerceAtLeast(0)
        val nextIndex = if (forward) {
            (currentIndex + 1) % browser.tabs.size
        } else {
            (currentIndex - 1 + browser.tabs.size) % browser.tabs.size
        }
        selectBrowserTab(browser.tabs[nextIndex].id)
    }

    override fun duplicateBrowserTab(tabId: String) {
        val source = _uiState.value.browser.tabs.firstOrNull { it.id == tabId } ?: return
        val id = nextBrowserTabId()
        updateBrowser(persist = true) { browser ->
            val duplicate = source.copy(
                id = id,
                title = "${source.title} copy",
                progress = 0,
                isLoading = true,
                canGoBack = false,
                canGoForward = false,
                renderGeneration = 0,
                lastError = null,
            )
            browser.copy(
                tabs = (browser.tabs.takeLast(MAX_BROWSER_TABS - 1) + duplicate),
                activeTabId = id,
                addressInput = duplicate.url,
                panel = BrowserPanel.NONE,
                command = null,
            )
        }
    }

    override fun reopenClosedBrowserTab() {
        val closed = _uiState.value.browser.closedTabs.firstOrNull() ?: run {
            showMessage("No recently closed tab")
            return
        }
        val reopened = closed.copy(
            id = nextBrowserTabId(),
            progress = 0,
            isLoading = true,
            canGoBack = false,
            canGoForward = false,
            renderGeneration = 0,
            lastError = null,
        )
        openSurface(StageSurface.BROWSER)
        updateBrowser(persist = true) { browser ->
            browser.copy(
                tabs = (browser.tabs.takeLast(MAX_BROWSER_TABS - 1) + reopened),
                activeTabId = reopened.id,
                addressInput = reopened.url,
                closedTabs = browser.closedTabs.drop(1),
                panel = BrowserPanel.NONE,
                command = null,
            )
        }
    }

    override fun navigateBrowser(url: String) {
        val target = normalizeBrowserInput(url)
        updateBrowser(persist = true) { browser ->
            val tab = browser.activeTab
            val profile = profileForUrl(browser, target, tab.profile)
            browser.copy(
                tabs = browser.tabs.replaceTab(
                    tab.copy(
                        url = target,
                        profile = profile,
                        progress = 0,
                        isLoading = true,
                        lastError = null,
                    ),
                ),
                addressInput = target,
                panel = BrowserPanel.NONE,
                command = browserCommand(tab.id, BrowserCommandType.NAVIGATE, target),
            )
        }
    }

    override fun updateBrowserAddress(value: String) {
        updateBrowser { it.copy(addressInput = value) }
    }

    override fun submitBrowserAddress() {
        val input = _uiState.value.browser.addressInput.trim()
        val scheme = runCatching { input.toUri().scheme?.lowercase(Locale.ROOT) }.getOrNull()
        if (scheme in EXTERNAL_BROWSER_SCHEMES || scheme == "intent") {
            requestExternalNavigation(input)
        } else {
            navigateBrowser(input)
        }
    }

    override fun requestBrowserCommand(type: BrowserCommandType, payload: String?) {
        updateBrowser { browser ->
            browser.copy(command = browserCommand(browser.activeTabId, type, payload))
        }
    }

    override fun consumeBrowserCommand(serial: Long) {
        updateBrowser { browser ->
            if (browser.command?.serial == serial) browser.copy(command = null) else browser
        }
    }

    override fun setBrowserProfile(profile: BrowserProfile) {
        updateBrowser(persist = true) { browser ->
            val tab = browser.activeTab
            val host = runCatching { tab.url.toUri().host?.lowercase(Locale.ROOT) }.getOrNull()
            val profiles = if (host.isNullOrBlank()) {
                browser.siteProfiles
            } else {
                browser.siteProfiles + (host to profile)
            }
            browser.copy(
                tabs = browser.tabs.replaceTab(tab.copy(profile = profile)),
                siteProfiles = profiles,
                panel = BrowserPanel.NONE,
                command = browserCommand(tab.id, BrowserCommandType.RELOAD),
            )
        }
    }

    override fun setBrowserPanel(panel: BrowserPanel) {
        updateBrowser { it.copy(panel = panel) }
        if (panel == BrowserPanel.DOWNLOADS) refreshBrowserDownloads()
    }

    override fun setBrowserFocusMode(enabled: Boolean) {
        updateBrowser { it.copy(focusMode = enabled, panel = BrowserPanel.NONE) }
    }

    override fun setBrowserFindVisible(visible: Boolean) {
        updateBrowser { browser ->
            browser.copy(
                findBarVisible = visible,
                findQuery = if (visible) browser.findQuery else "",
                panel = BrowserPanel.NONE,
                command = if (visible) {
                    browser.command
                } else {
                    browserCommand(browser.activeTabId, BrowserCommandType.CLEAR_FIND)
                },
            )
        }
    }

    override fun setBrowserFindQuery(query: String) {
        updateBrowser { browser ->
            browser.copy(
                findQuery = query,
                command = browserCommand(
                    browser.activeTabId,
                    if (query.isBlank()) BrowserCommandType.CLEAR_FIND else BrowserCommandType.FIND_NEXT,
                    query,
                ),
            )
        }
    }

    override fun toggleActiveBrowserBookmark() {
        updateBrowser(persist = true) { browser ->
            val tab = browser.activeTab
            val existing = browser.bookmarks.any { it.url == tab.url }
            val bookmarks = if (existing) {
                browser.bookmarks.filterNot { it.url == tab.url }
            } else {
                listOf(
                    BrowserBookmark(
                        url = tab.url,
                        title = tab.title.ifBlank { tab.url },
                        createdAt = System.currentTimeMillis(),
                    ),
                ) + browser.bookmarks
            }
            browser.copy(bookmarks = bookmarks.take(MAX_BOOKMARKS))
        }
    }

    override fun removeBrowserBookmark(url: String) {
        updateBrowser(persist = true) { browser ->
            browser.copy(bookmarks = browser.bookmarks.filterNot { it.url == url })
        }
    }

    override fun reportBrowserPage(tabId: String, snapshot: BrowserPageSnapshot) {
        val existing = _uiState.value.browser.tabs.firstOrNull { it.id == tabId } ?: return
        val safeUrl = snapshot.url.ifBlank { existing.url }
        val title = snapshot.title.ifBlank {
            runCatching { safeUrl.toUri().host }.getOrNull() ?: "New Tab"
        }
        val persistSnapshot = snapshot.finished ||
            (!snapshot.isLoading && snapshot.scrollY != existing.scrollY)
        updateBrowser(persist = persistSnapshot) { browser ->
            val current = browser.tabs.firstOrNull { it.id == tabId } ?: return@updateBrowser browser
            val updated = current.copy(
                title = title,
                url = safeUrl,
                progress = snapshot.progress.coerceIn(0, 100),
                isLoading = snapshot.isLoading,
                canGoBack = snapshot.canGoBack,
                canGoForward = snapshot.canGoForward,
                scrollY = snapshot.scrollY,
                lastError = if (snapshot.isLoading && snapshot.progress <= 10) {
                    null
                } else {
                    current.lastError
                },
            )
            val history = if (snapshot.finished && safeUrl.startsWith("http")) {
                listOf(
                    BrowserHistoryEntry(
                        url = safeUrl,
                        title = title,
                        visitedAt = System.currentTimeMillis(),
                    ),
                ) + browser.history.filterNot { it.url == safeUrl }
            } else {
                browser.history
            }
            browser.copy(
                tabs = browser.tabs.replaceTab(updated),
                addressInput = if (browser.activeTabId == tabId) safeUrl else browser.addressInput,
                history = history.take(MAX_HISTORY),
            )
        }
    }

    override fun reportBrowserError(tabId: String, message: String) {
        updateBrowser { browser ->
            val tab = browser.tabs.firstOrNull { it.id == tabId } ?: return@updateBrowser browser
            browser.copy(
                tabs = browser.tabs.replaceTab(
                    tab.copy(isLoading = false, progress = 0, lastError = message),
                ),
            )
        }
    }

    override fun reportBrowserRendererGone(tabId: String, didCrash: Boolean) {
        updateBrowser(persist = true) { browser ->
            val tab = browser.tabs.firstOrNull { it.id == tabId } ?: return@updateBrowser browser
            val updated = tab.copy(
                renderGeneration = tab.renderGeneration + 1,
                isLoading = true,
                progress = 0,
                lastError = if (didCrash) "The page renderer crashed. Stage restored this tab."
                else "Android reclaimed the page renderer. Stage restored this tab.",
            )
            browser.copy(
                tabs = browser.tabs.replaceTab(updated),
                command = browser.command,
            )
        }
        showMessage(if (didCrash) "Browser renderer recovered" else "Background tab restored")
    }

    override fun requestExternalNavigation(url: String) {
        updateBrowser { it.copy(pendingExternalUrl = url) }
    }

    override fun dismissExternalNavigation() {
        updateBrowser { it.copy(pendingExternalUrl = null) }
    }

    override fun confirmExternalNavigation() {
        val url = _uiState.value.browser.pendingExternalUrl ?: return
        dismissExternalNavigation()
        browserPlatform.openExternal(url).onFailure { failure ->
            showError("No application can open this link", failure)
        }
    }

    override fun enqueueBrowserDownload(request: BrowserDownloadRequest) {
        browserPlatform.enqueueDownload(request).onSuccess { download ->
            updateBrowser(persist = true) { browser ->
                browser.copy(
                    downloads = (listOf(download) + browser.downloads).take(MAX_DOWNLOADS),
                    panel = BrowserPanel.DOWNLOADS,
                )
            }
            showMessage("Downloading ${download.fileName}")
        }.onFailure { failure ->
            showError("Download could not start", failure)
        }
    }

    override fun refreshBrowserDownloads() {
        val snapshot = _uiState.value.browser.downloads
        if (snapshot.isEmpty()) return
        viewModelScope.launch {
            val refreshed = runCatching { browserPlatform.refreshDownloads(snapshot) }
                .getOrElse { failure ->
                    showError("Downloads could not be refreshed", failure)
                    return@launch
                }
            val byId = refreshed.associateBy { it.id }
            updateBrowser(persist = true) { browser ->
                browser.copy(
                    downloads = browser.downloads.map { byId[it.id] ?: it },
                )
            }
        }
    }

    override fun cancelBrowserDownload(id: Long) {
        browserPlatform.cancelDownload(id).onSuccess {
            updateBrowser(persist = true) { browser ->
                browser.copy(
                    downloads = browser.downloads.map { download ->
                        if (download.id == id) {
                            download.copy(status = BrowserDownloadStatus.CANCELLED)
                        } else {
                            download
                        }
                    },
                )
            }
        }.onFailure { failure -> showError("Download could not be cancelled", failure) }
    }

    override fun openBrowserDownload(id: Long) {
        val download = _uiState.value.browser.downloads.firstOrNull { it.id == id } ?: return
        browserPlatform.openDownload(download).onFailure { failure ->
            showError("Downloaded file could not be opened", failure)
        }
    }

    override fun shareBrowserDownload(id: Long) {
        val download = _uiState.value.browser.downloads.firstOrNull { it.id == id } ?: return
        browserPlatform.shareDownload(download).onFailure { failure ->
            showError("Downloaded file could not be shared", failure)
        }
    }

    override fun shareActiveBrowserPage() {
        val tab = _uiState.value.browser.activeTab
        browserPlatform.sharePage(tab.title, tab.url).onFailure { failure ->
            showError("Page could not be shared", failure)
        }
    }

    override fun refreshPlatformState() {
        refreshStatusOnly()
        installedApps.refresh()
    }

    override fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    private fun refreshStatusOnly() {
        val application = getApplication<Application>()
        val roleManager = application.getSystemService(RoleManager::class.java)
        val homeAvailable = roleManager.isRoleAvailable(RoleManager.ROLE_HOME)
        val homeHeld = homeAvailable && roleManager.isRoleHeld(RoleManager.ROLE_HOME)
        _uiState.value = _uiState.value.copy(
            homeRoleAvailable = homeAvailable,
            homeRoleHeld = homeHeld,
            systemStatus = readSystemStatus(application),
        )
    }

    private fun readSystemStatus(context: Context): SystemStatus {
        val batteryIntent = ContextCompat.registerReceiver(
            context,
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val batteryPercent = if (level >= 0 && scale > 0) level * 100 / scale else 0
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        val isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        val formatter = DateTimeFormatter.ofPattern("EEE d MMM  HH:mm", Locale.getDefault())
        return SystemStatus(
            clockLabel = LocalDateTime.now().format(formatter),
            batteryPercent = batteryPercent,
            isOnline = isOnline,
        )
    }

    private fun updateBrowser(
        persist: Boolean = false,
        transform: (BrowserUiState) -> BrowserUiState,
    ) {
        val updated = transform(_uiState.value.browser)
        _uiState.value = _uiState.value.copy(browser = updated)
        if (persist) persistBrowser(updated)
    }

    private fun persistCurrentBrowser() {
        persistBrowser(_uiState.value.browser)
    }

    private fun persistBrowser(browser: BrowserUiState) {
        viewModelScope.launch {
            browserSaveMutex.withLock {
                browserSession.save(browser)
            }
        }
    }

    private fun browserCommand(
        tabId: String,
        type: BrowserCommandType,
        payload: String? = null,
    ): BrowserCommand = BrowserCommand(
        serial = ++browserCommandSerial,
        tabId = tabId,
        type = type,
        payload = payload,
    )

    private fun nextBrowserTabId(): String =
        "tab-${System.currentTimeMillis().toString(36)}-${++browserTabSerial}"

    private fun profileForUrl(
        browser: BrowserUiState,
        url: String,
        fallback: BrowserProfile = BrowserProfile.DESKTOP,
    ): BrowserProfile {
        val host = runCatching { url.toUri().host?.lowercase(Locale.ROOT) }.getOrNull()
        return host?.let(browser.siteProfiles::get) ?: fallback
    }

    private fun List<BrowserTab>.replaceTab(updated: BrowserTab): List<BrowserTab> =
        map { tab -> if (tab.id == updated.id) updated else tab }

    private fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }

    private fun showError(message: String, failure: Throwable) {
        _uiState.value = _uiState.value.copy(
            message = "$message: ${failure.localizedMessage ?: "unknown error"}",
        )
    }

    override fun onCleared() {
        installedApps.stop()
    }

    private companion object {
        const val MAX_BROWSER_TABS = 20
        const val MAX_CLOSED_TABS = 10
        const val MAX_BOOKMARKS = 100
        const val MAX_HISTORY = 150
        const val MAX_DOWNLOADS = 50
        val EXTERNAL_BROWSER_SCHEMES = setOf("mailto", "tel", "geo", "market")
    }

    class Factory(
        private val application: Application,
        private val initialProfile: DeviceProfile,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(StageShellViewModel::class.java))
            return StageShellViewModel(application, initialProfile) as T
        }
    }
}
