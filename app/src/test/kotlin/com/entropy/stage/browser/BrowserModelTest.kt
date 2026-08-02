package com.entropy.stage.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserModelTest {
    @Test
    fun addressInputDistinguishesUrlsFromSearchQueries() {
        assertEquals("https://developer.android.com", normalizeBrowserInput("developer.android.com"))
        assertEquals("http://localhost:8080", normalizeBrowserInput("http://localhost:8080"))
        assertEquals(
            "https://www.google.com/search?q=stage+desktop+browser",
            normalizeBrowserInput("stage desktop browser"),
        )
    }

    @Test
    fun tabCodecPreservesUnicodeAndSeparators() {
        val tabs = listOf(
            BrowserTab(
                id = "tab|one",
                title = "Stage | research\n✓",
                url = "https://example.com/a?query=one|two",
                profile = BrowserProfile.ADAPTIVE,
                scrollY = 842,
            ),
        )

        val decoded = decodeTabs(encodeTabs(tabs))

        assertEquals(tabs.single().id, decoded.single().id)
        assertEquals(tabs.single().title, decoded.single().title)
        assertEquals(tabs.single().url, decoded.single().url)
        assertEquals(BrowserProfile.ADAPTIVE, decoded.single().profile)
        assertEquals(842, decoded.single().scrollY)
    }

    @Test
    fun downloadCodecKeepsRealStatusAndProgress() {
        val download = BrowserDownload(
            id = 72L,
            url = "https://example.com/stage.apk",
            fileName = "stage.apk",
            mimeType = "application/vnd.android.package-archive",
            createdAt = 42L,
            status = BrowserDownloadStatus.RUNNING,
            bytesDownloaded = 512,
            totalBytes = 1024,
            localUri = "content://downloads/72",
        )

        assertEquals(download, decodeDownloads(encodeDownloads(listOf(download))).single())
    }

    @Test
    fun desktopUserAgentRemovesAndroidMobileIdentity() {
        val desktop = desktopUserAgent(
            "Mozilla/5.0 (Linux; Android 11) AppleWebKit/537.36 " +
                "Chrome/140.0.7339.51 Mobile Safari/537.36",
        )

        assertTrue(desktop.contains("X11; Linux x86_64"))
        assertTrue(desktop.contains("Chrome/140.0.7339.51"))
        assertFalse(desktop.contains("Android"))
        assertFalse(desktop.contains("Mobile"))
    }
}
