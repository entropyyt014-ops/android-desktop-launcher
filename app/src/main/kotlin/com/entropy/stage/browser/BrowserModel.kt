package com.entropy.stage.browser

const val BROWSER_HOME_URL = "https://www.google.com/"

enum class BrowserProfile(val label: String) {
    DESKTOP("Desktop"),
    ADAPTIVE("Adaptive"),
    MOBILE("Mobile"),
}

enum class BrowserPanel {
    NONE,
    TABS,
    MENU,
    PROFILES,
    BOOKMARKS,
    HISTORY,
    DOWNLOADS,
}

enum class BrowserCommandType {
    NAVIGATE,
    BACK,
    FORWARD,
    RELOAD,
    STOP,
    FOCUS_ADDRESS,
    FIND_NEXT,
    FIND_PREVIOUS,
    CLEAR_FIND,
}

enum class BrowserDownloadStatus {
    PENDING,
    RUNNING,
    PAUSED,
    SUCCESSFUL,
    FAILED,
    CANCELLED,
}

data class BrowserTab(
    val id: String,
    val title: String = "New Tab",
    val url: String = BROWSER_HOME_URL,
    val profile: BrowserProfile = BrowserProfile.DESKTOP,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val renderGeneration: Int = 0,
    val scrollY: Int = 0,
    val lastError: String? = null,
)

data class BrowserBookmark(
    val url: String,
    val title: String,
    val createdAt: Long,
)

data class BrowserHistoryEntry(
    val url: String,
    val title: String,
    val visitedAt: Long,
)

data class BrowserDownload(
    val id: Long,
    val url: String,
    val fileName: String,
    val mimeType: String?,
    val createdAt: Long,
    val status: BrowserDownloadStatus = BrowserDownloadStatus.PENDING,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = -1,
    val localUri: String? = null,
    val failureReason: Int? = null,
)

data class BrowserCommand(
    val serial: Long,
    val tabId: String,
    val type: BrowserCommandType,
    val payload: String? = null,
)

data class BrowserPageSnapshot(
    val url: String,
    val title: String,
    val progress: Int,
    val isLoading: Boolean,
    val canGoBack: Boolean,
    val canGoForward: Boolean,
    val scrollY: Int = 0,
    val finished: Boolean = false,
)

data class BrowserDownloadRequest(
    val url: String,
    val userAgent: String?,
    val contentDisposition: String?,
    val mimeType: String?,
    val contentLength: Long,
    val referringUrl: String?,
)

data class BrowserUiState(
    val tabs: List<BrowserTab> = listOf(BrowserTab(id = "tab-1")),
    val activeTabId: String = "tab-1",
    val closedTabs: List<BrowserTab> = emptyList(),
    val addressInput: String = BROWSER_HOME_URL,
    val panel: BrowserPanel = BrowserPanel.NONE,
    val focusMode: Boolean = false,
    val findBarVisible: Boolean = false,
    val findQuery: String = "",
    val command: BrowserCommand? = null,
    val bookmarks: List<BrowserBookmark> = emptyList(),
    val history: List<BrowserHistoryEntry> = emptyList(),
    val downloads: List<BrowserDownload> = emptyList(),
    val siteProfiles: Map<String, BrowserProfile> = emptyMap(),
    val pendingExternalUrl: String? = null,
    val sessionLoaded: Boolean = false,
) {
    val activeTab: BrowserTab
        get() = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()

    val activePageBookmarked: Boolean
        get() = bookmarks.any { it.url == activeTab.url }

    companion object {
        fun preview(): BrowserUiState = BrowserUiState(
            tabs = listOf(
                BrowserTab(
                    id = "preview-stage",
                    title = "Stage Browser",
                    url = "https://developer.android.com/",
                    profile = BrowserProfile.DESKTOP,
                    progress = 100,
                ),
                BrowserTab(
                    id = "preview-research",
                    title = "Research",
                    url = "https://www.google.com/",
                    profile = BrowserProfile.ADAPTIVE,
                    progress = 100,
                ),
            ),
            activeTabId = "preview-stage",
            addressInput = "developer.android.com",
            sessionLoaded = true,
        )
    }
}

interface BrowserActions {
    fun newBrowserTab(url: String? = null)
    fun closeBrowserTab(tabId: String)
    fun selectBrowserTab(tabId: String)
    fun cycleBrowserTab(forward: Boolean)
    fun duplicateBrowserTab(tabId: String)
    fun reopenClosedBrowserTab()
    fun navigateBrowser(url: String)
    fun updateBrowserAddress(value: String)
    fun submitBrowserAddress()
    fun requestBrowserCommand(type: BrowserCommandType, payload: String? = null)
    fun consumeBrowserCommand(serial: Long)
    fun setBrowserProfile(profile: BrowserProfile)
    fun setBrowserPanel(panel: BrowserPanel)
    fun setBrowserFocusMode(enabled: Boolean)
    fun setBrowserFindVisible(visible: Boolean)
    fun setBrowserFindQuery(query: String)
    fun toggleActiveBrowserBookmark()
    fun removeBrowserBookmark(url: String)
    fun reportBrowserPage(tabId: String, snapshot: BrowserPageSnapshot)
    fun reportBrowserError(tabId: String, message: String)
    fun reportBrowserRendererGone(tabId: String, didCrash: Boolean)
    fun requestExternalNavigation(url: String)
    fun dismissExternalNavigation()
    fun confirmExternalNavigation()
    fun enqueueBrowserDownload(request: BrowserDownloadRequest)
    fun refreshBrowserDownloads()
    fun cancelBrowserDownload(id: Long)
    fun openBrowserDownload(id: Long)
    fun shareBrowserDownload(id: Long)
    fun shareActiveBrowserPage()
    fun openActiveBrowserPageExternally()
}

fun normalizeBrowserInput(rawInput: String): String {
    val input = rawInput.trim()
    if (input.isEmpty()) return BROWSER_HOME_URL

    val lower = input.lowercase()
    if (lower.startsWith("https://") || lower.startsWith("http://")) return input

    val looksLikeHost = !input.any(Char::isWhitespace) &&
        (input.contains('.') || input.startsWith("localhost", ignoreCase = true))
    if (looksLikeHost) return "https://$input"

    return "https://www.google.com/search?q=${java.net.URLEncoder.encode(input, Charsets.UTF_8.name())}"
}
