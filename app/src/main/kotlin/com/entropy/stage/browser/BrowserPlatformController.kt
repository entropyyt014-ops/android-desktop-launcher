package com.entropy.stage.browser

import android.app.Application
import android.app.DownloadManager
import android.content.Intent
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import androidx.core.net.toUri
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BrowserPlatformController(
    private val application: Application,
) {
    private val downloadManager = application.getSystemService(DownloadManager::class.java)

    fun enqueueDownload(request: BrowserDownloadRequest): Result<BrowserDownload> = runCatching {
        val uri = request.url.toUri()
        require(uri.scheme == "https" || uri.scheme == "http") {
            "Only HTTP and HTTPS downloads are supported"
        }
        val guessedName = URLUtil.guessFileName(
            request.url,
            request.contentDisposition,
            request.mimeType,
        )
        val fileName = sanitizeFileName(guessedName)
        val managerRequest = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription(uri.host ?: "Stage Browser")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.mimeType?.takeIf(String::isNotBlank)?.let(managerRequest::setMimeType)
        request.userAgent?.takeIf(String::isNotBlank)?.let {
            managerRequest.addRequestHeader("User-Agent", it)
        }
        request.referringUrl?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
            ?.let { managerRequest.addRequestHeader("Referer", it) }
        CookieManager.getInstance().getCookie(request.url)?.takeIf(String::isNotBlank)?.let {
            managerRequest.addRequestHeader("Cookie", it)
        }
        val id = downloadManager.enqueue(managerRequest)
        BrowserDownload(
            id = id,
            url = request.url,
            fileName = fileName,
            mimeType = request.mimeType,
            createdAt = System.currentTimeMillis(),
        )
    }

    suspend fun refreshDownloads(downloads: List<BrowserDownload>): List<BrowserDownload> =
        withContext(Dispatchers.IO) {
            downloads.map(::queryDownload)
        }

    fun cancelDownload(id: Long): Result<Unit> = runCatching {
        downloadManager.remove(id)
        Unit
    }

    fun openDownload(download: BrowserDownload): Result<Unit> = runCatching {
        val uri = downloadManager.getUriForDownloadedFile(download.id)
            ?: download.localUri?.toUri()?.takeIf { it.scheme == "content" }
            ?: error("The downloaded file is not available")
        application.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, download.mimeType ?: "*/*")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION),
        )
    }

    fun shareDownload(download: BrowserDownload): Result<Unit> = runCatching {
        val uri = downloadManager.getUriForDownloadedFile(download.id)
            ?: download.localUri?.toUri()?.takeIf { it.scheme == "content" }
            ?: error("The downloaded file is not available")
        val share = Intent(Intent.ACTION_SEND)
            .setType(download.mimeType ?: "*/*")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        application.startActivity(
            Intent.createChooser(share, "Share ${download.fileName}")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun sharePage(title: String, url: String): Result<Unit> = runCatching {
        val share = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_SUBJECT, title)
            .putExtra(Intent.EXTRA_TEXT, url)
        application.startActivity(
            Intent.createChooser(share, "Share page").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openExternal(url: String): Result<Unit> = runCatching {
        val uri = url.toUri()
        val intent = if (uri.scheme.equals("intent", ignoreCase = true)) {
            Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                component = null
                selector = null
            }
        } else {
            require(uri.scheme?.lowercase(Locale.ROOT) in ALLOWED_EXTERNAL_SCHEMES) {
                "Stage blocked an unsupported link type"
            }
            Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        }
        application.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun queryDownload(download: BrowserDownload): BrowserDownload {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(download.id))
        cursor.use {
            if (!it.moveToFirst()) return download.copy(status = BrowserDownloadStatus.CANCELLED)
            val status = when (it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_PENDING -> BrowserDownloadStatus.PENDING
                DownloadManager.STATUS_RUNNING -> BrowserDownloadStatus.RUNNING
                DownloadManager.STATUS_PAUSED -> BrowserDownloadStatus.PAUSED
                DownloadManager.STATUS_SUCCESSFUL -> BrowserDownloadStatus.SUCCESSFUL
                DownloadManager.STATUS_FAILED -> BrowserDownloadStatus.FAILED
                else -> BrowserDownloadStatus.FAILED
            }
            val localUri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
            val reason = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return download.copy(
                status = status,
                bytesDownloaded = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
                ),
                totalBytes = it.getLong(
                    it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
                ),
                localUri = localUri,
                failureReason = reason.takeIf { status == BrowserDownloadStatus.FAILED },
            )
        }
    }

    private fun sanitizeFileName(name: String): String {
        val sanitized = name
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]"), "_")
            .trim()
            .take(120)
        return sanitized.ifBlank { "stage-download-${System.currentTimeMillis()}" }
    }

    private companion object {
        val ALLOWED_EXTERNAL_SCHEMES = setOf("mailto", "tel", "geo", "market")
    }
}
