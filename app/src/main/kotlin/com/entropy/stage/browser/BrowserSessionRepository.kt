package com.entropy.stage.browser

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.Base64
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

private val Context.browserDataStore by preferencesDataStore(name = "stage_browser")

data class BrowserPersistedState(
    val tabs: List<BrowserTab>,
    val activeTabId: String,
    val closedTabs: List<BrowserTab>,
    val bookmarks: List<BrowserBookmark>,
    val history: List<BrowserHistoryEntry>,
    val downloads: List<BrowserDownload>,
    val siteProfiles: Map<String, BrowserProfile>,
)

class BrowserSessionRepository(context: Context) {
    private val dataStore = context.browserDataStore

    suspend fun load(): BrowserPersistedState {
        val preferences = dataStore.data
            .catch { throwable ->
                if (throwable is IOException) emit(emptyPreferences()) else throw throwable
            }
            .first()
        return decodeBrowserState(preferences)
    }

    suspend fun save(state: BrowserUiState) {
        dataStore.edit { preferences ->
            preferences[Keys.Tabs] = encodeTabs(state.tabs.take(MAX_TABS))
            preferences[Keys.ActiveTab] = state.activeTabId
            preferences[Keys.ClosedTabs] = encodeTabs(state.closedTabs.take(MAX_CLOSED_TABS))
            preferences[Keys.Bookmarks] = encodeBookmarks(state.bookmarks.take(MAX_BOOKMARKS))
            preferences[Keys.History] = encodeHistory(state.history.take(MAX_HISTORY))
            preferences[Keys.Downloads] = encodeDownloads(state.downloads.take(MAX_DOWNLOADS))
            preferences[Keys.SiteProfiles] = encodeSiteProfiles(state.siteProfiles.entries.take(MAX_SITES))
        }
    }

    private object Keys {
        val Tabs = stringPreferencesKey("tabs")
        val ActiveTab = stringPreferencesKey("active_tab")
        val ClosedTabs = stringPreferencesKey("closed_tabs")
        val Bookmarks = stringPreferencesKey("bookmarks")
        val History = stringPreferencesKey("history")
        val Downloads = stringPreferencesKey("downloads")
        val SiteProfiles = stringPreferencesKey("site_profiles")
    }

    private companion object {
        const val MAX_TABS = 20
        const val MAX_CLOSED_TABS = 10
        const val MAX_BOOKMARKS = 100
        const val MAX_HISTORY = 150
        const val MAX_DOWNLOADS = 50
        const val MAX_SITES = 100
    }
}

internal fun decodeBrowserState(preferences: Preferences): BrowserPersistedState {
    val restoredTabs = decodeTabs(preferences[stringPreferencesKey("tabs")])
    val tabs = restoredTabs.ifEmpty { listOf(BrowserTab(id = "tab-1")) }
    val savedActive = preferences[stringPreferencesKey("active_tab")]
    val active = savedActive?.takeIf { id -> tabs.any { it.id == id } } ?: tabs.first().id
    return BrowserPersistedState(
        tabs = tabs,
        activeTabId = active,
        closedTabs = decodeTabs(preferences[stringPreferencesKey("closed_tabs")]),
        bookmarks = decodeBookmarks(preferences[stringPreferencesKey("bookmarks")]),
        history = decodeHistory(preferences[stringPreferencesKey("history")]),
        downloads = decodeDownloads(preferences[stringPreferencesKey("downloads")]),
        siteProfiles = decodeSiteProfiles(preferences[stringPreferencesKey("site_profiles")]),
    )
}

internal fun encodeTabs(tabs: List<BrowserTab>): String = tabs.joinToString("\n") { tab ->
    listOf(
        encodeField(tab.id),
        encodeField(tab.title),
        encodeField(tab.url),
        tab.profile.name,
        tab.scrollY.toString(),
    ).joinToString("|")
}

internal fun decodeTabs(encoded: String?): List<BrowserTab> = encoded.orEmpty()
    .lineSequence()
    .mapNotNull { record ->
        val fields = record.split('|')
        if (fields.size != 5) return@mapNotNull null
        val id = decodeField(fields[0]) ?: return@mapNotNull null
        val title = decodeField(fields[1]) ?: return@mapNotNull null
        val url = decodeField(fields[2]) ?: return@mapNotNull null
        val profile = runCatching { BrowserProfile.valueOf(fields[3]) }.getOrNull()
            ?: BrowserProfile.DESKTOP
        BrowserTab(
            id = id,
            title = title,
            url = url,
            profile = profile,
            progress = 0,
            scrollY = fields[4].toIntOrNull() ?: 0,
        )
    }
    .toList()

internal fun encodeBookmarks(bookmarks: List<BrowserBookmark>): String =
    bookmarks.joinToString("\n") { bookmark ->
        listOf(
            encodeField(bookmark.url),
            encodeField(bookmark.title),
            bookmark.createdAt.toString(),
        ).joinToString("|")
    }

internal fun decodeBookmarks(encoded: String?): List<BrowserBookmark> = encoded.orEmpty()
    .lineSequence()
    .mapNotNull { record ->
        val fields = record.split('|')
        if (fields.size != 3) return@mapNotNull null
        BrowserBookmark(
            url = decodeField(fields[0]) ?: return@mapNotNull null,
            title = decodeField(fields[1]) ?: return@mapNotNull null,
            createdAt = fields[2].toLongOrNull() ?: 0L,
        )
    }
    .toList()

internal fun encodeHistory(history: List<BrowserHistoryEntry>): String =
    history.joinToString("\n") { entry ->
        listOf(
            encodeField(entry.url),
            encodeField(entry.title),
            entry.visitedAt.toString(),
        ).joinToString("|")
    }

internal fun decodeHistory(encoded: String?): List<BrowserHistoryEntry> = encoded.orEmpty()
    .lineSequence()
    .mapNotNull { record ->
        val fields = record.split('|')
        if (fields.size != 3) return@mapNotNull null
        BrowserHistoryEntry(
            url = decodeField(fields[0]) ?: return@mapNotNull null,
            title = decodeField(fields[1]) ?: return@mapNotNull null,
            visitedAt = fields[2].toLongOrNull() ?: 0L,
        )
    }
    .toList()

internal fun encodeDownloads(downloads: List<BrowserDownload>): String =
    downloads.joinToString("\n") { download ->
        listOf(
            download.id.toString(),
            encodeField(download.url),
            encodeField(download.fileName),
            encodeField(download.mimeType.orEmpty()),
            download.createdAt.toString(),
            download.status.name,
            download.bytesDownloaded.toString(),
            download.totalBytes.toString(),
            encodeField(download.localUri.orEmpty()),
            download.failureReason?.toString().orEmpty(),
        ).joinToString("|")
    }

internal fun decodeDownloads(encoded: String?): List<BrowserDownload> = encoded.orEmpty()
    .lineSequence()
    .mapNotNull { record ->
        val fields = record.split('|')
        if (fields.size != 10) return@mapNotNull null
        BrowserDownload(
            id = fields[0].toLongOrNull() ?: return@mapNotNull null,
            url = decodeField(fields[1]) ?: return@mapNotNull null,
            fileName = decodeField(fields[2]) ?: return@mapNotNull null,
            mimeType = decodeField(fields[3])?.ifBlank { null },
            createdAt = fields[4].toLongOrNull() ?: 0L,
            status = runCatching { BrowserDownloadStatus.valueOf(fields[5]) }.getOrNull()
                ?: BrowserDownloadStatus.PENDING,
            bytesDownloaded = fields[6].toLongOrNull() ?: 0L,
            totalBytes = fields[7].toLongOrNull() ?: -1L,
            localUri = decodeField(fields[8])?.ifBlank { null },
            failureReason = fields[9].toIntOrNull(),
        )
    }
    .toList()

internal fun encodeSiteProfiles(entries: List<Map.Entry<String, BrowserProfile>>): String =
    entries.joinToString("\n") { entry -> "${encodeField(entry.key)}|${entry.value.name}" }

internal fun decodeSiteProfiles(encoded: String?): Map<String, BrowserProfile> = encoded.orEmpty()
    .lineSequence()
    .mapNotNull { record ->
        val fields = record.split('|')
        if (fields.size != 2) return@mapNotNull null
        val host = decodeField(fields[0]) ?: return@mapNotNull null
        val profile = runCatching { BrowserProfile.valueOf(fields[1]) }.getOrNull()
            ?: return@mapNotNull null
        host to profile
    }
    .toMap(LinkedHashMap())

private fun encodeField(value: String): String = Base64.getUrlEncoder()
    .withoutPadding()
    .encodeToString(value.toByteArray(Charsets.UTF_8))

private fun decodeField(value: String): String? = runCatching {
    String(Base64.getUrlDecoder().decode(value), Charsets.UTF_8)
}.getOrNull()
