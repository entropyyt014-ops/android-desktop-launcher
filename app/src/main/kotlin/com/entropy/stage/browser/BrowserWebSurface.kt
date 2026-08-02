package com.entropy.stage.browser

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.entropy.stage.designsystem.StagePalette
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun BrowserWebSurface(
    state: BrowserUiState,
    actions: BrowserActions,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val pool = remember(context) { BrowserWebViewPool(context) }
    val restoredScrollTabs = remember { mutableSetOf<String>() }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    var fileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    var pendingPermission by remember { mutableStateOf<PendingSitePermission?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val callback = fileCallback
        fileCallback = null
        callback?.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data),
        )
    }

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val prompt = pendingPermission
        pendingPermission = null
        when (prompt) {
            is PendingSitePermission.Media -> {
                val required = androidPermissionsFor(prompt.request.resources)
                if (required.all { permission ->
                        grants[permission] == true ||
                            ContextCompat.checkSelfPermission(context, permission) ==
                            PackageManager.PERMISSION_GRANTED
                    }
                ) {
                    prompt.request.grant(allowedWebResources(prompt.request.resources))
                } else {
                    prompt.request.deny()
                }
            }

            is PendingSitePermission.Location -> {
                val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
                prompt.callback.invoke(prompt.origin, granted, false)
            }

            null -> Unit
        }
    }

    val tab = state.activeTab
    LaunchedEffect(state.tabs.map(BrowserTab::id)) {
        pool.retainTabs(state.tabs.mapTo(mutableSetOf(), BrowserTab::id))
    }
    key(tab.id, tab.renderGeneration) {
        AndroidView(
            factory = {
                pool.obtain(tab).also { webView ->
                    bindBrowserClients(
                        webView = webView,
                        tab = tab,
                        actions = actions,
                        pool = pool,
                        onFileChooser = { callback, params ->
                            fileCallback?.onReceiveValue(null)
                            fileCallback = callback
                            runCatching { fileChooserLauncher.launch(params.createIntent()) }
                                .onFailure {
                                    fileCallback = null
                                    callback.onReceiveValue(null)
                                }
                        },
                        onPermissionPrompt = {
                            pendingPermission?.deny()
                            pendingPermission = it
                        },
                        restoreScroll = {
                            if (restoredScrollTabs.add(tab.id) && tab.scrollY > 0) {
                                webView.post { webView.scrollTo(0, tab.scrollY) }
                            }
                        },
                    )
                    activeWebView = webView
                    pool.setActive(tab.id)
                    if (webView.url.isNullOrBlank()) webView.loadUrl(tab.url)
                }
            },
            update = { webView ->
                pool.applyProfile(webView, tab.profile)
                bindBrowserClients(
                    webView = webView,
                    tab = tab,
                    actions = actions,
                    pool = pool,
                    onFileChooser = { callback, params ->
                        fileCallback?.onReceiveValue(null)
                        fileCallback = callback
                        runCatching { fileChooserLauncher.launch(params.createIntent()) }
                            .onFailure {
                                fileCallback = null
                                callback.onReceiveValue(null)
                            }
                    },
                    onPermissionPrompt = {
                        pendingPermission?.deny()
                        pendingPermission = it
                    },
                    restoreScroll = {
                        if (restoredScrollTabs.add(tab.id) && tab.scrollY > 0) {
                            webView.post { webView.scrollTo(0, tab.scrollY) }
                        }
                    },
                )
                activeWebView = webView
                pool.setActive(tab.id)
            },
            onRelease = { webView ->
                actions.reportBrowserPage(tab.id, webView.snapshot(finished = false))
                webView.onPause()
                if (activeWebView === webView) activeWebView = null
            },
            modifier = modifier.fillMaxSize(),
        )
    }

    val command = state.command
    LaunchedEffect(command?.serial, activeWebView, tab.id) {
        val webView = activeWebView ?: return@LaunchedEffect
        val pending = command ?: return@LaunchedEffect
        if (pending.tabId != tab.id || pending.type == BrowserCommandType.FOCUS_ADDRESS) {
            return@LaunchedEffect
        }
        when (pending.type) {
            BrowserCommandType.NAVIGATE -> pending.payload?.let(webView::loadUrl)
            BrowserCommandType.BACK -> if (webView.canGoBack()) webView.goBack()
            BrowserCommandType.FORWARD -> if (webView.canGoForward()) webView.goForward()
            BrowserCommandType.RELOAD -> webView.reload()
            BrowserCommandType.STOP -> {
                webView.stopLoading()
                actions.reportBrowserPage(tab.id, webView.snapshot(isLoading = false))
            }
            BrowserCommandType.FIND_NEXT -> {
                if (pending.payload != null) webView.findAllAsync(pending.payload)
                else webView.findNext(true)
            }

            BrowserCommandType.FIND_PREVIOUS -> webView.findNext(false)
            BrowserCommandType.CLEAR_FIND -> webView.clearMatches()
            BrowserCommandType.FOCUS_ADDRESS -> Unit
        }
        actions.consumeBrowserCommand(pending.serial)
    }

    DisposableEffect(lifecycleOwner, pool) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> pool.resumeAll()
                Lifecycle.Event.ON_PAUSE -> pool.pauseAll()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            fileCallback?.onReceiveValue(null)
            fileCallback = null
            pendingPermission?.deny()
            pendingPermission = null
            pool.destroyAll()
        }
    }

    pendingPermission?.let { prompt ->
        SitePermissionDialog(
            prompt = prompt,
            onDeny = {
                prompt.deny()
                pendingPermission = null
            },
            onAllow = {
                val requiredPermissions = prompt.androidPermissions()
                val missing = requiredPermissions.filter { permission ->
                    ContextCompat.checkSelfPermission(context, permission) !=
                        PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) {
                    prompt.grant()
                    pendingPermission = null
                } else {
                    runtimePermissionLauncher.launch(missing.toTypedArray())
                }
            },
        )
    }
}

private fun bindBrowserClients(
    webView: WebView,
    tab: BrowserTab,
    actions: BrowserActions,
    pool: BrowserWebViewPool,
    onFileChooser: (ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams) -> Unit,
    onPermissionPrompt: (PendingSitePermission) -> Unit,
    restoreScroll: () -> Unit,
) {
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(
            view: WebView,
            request: WebResourceRequest,
        ): Boolean {
            if (!request.isForMainFrame) return false
            val scheme = request.url.scheme?.lowercase()
            if (scheme == "https" || scheme == "http") return false
            actions.requestExternalNavigation(request.url.toString())
            return true
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            actions.reportBrowserPage(tab.id, view.snapshot(url = url, isLoading = true))
        }

        override fun onPageFinished(view: WebView, url: String) {
            actions.reportBrowserPage(
                tab.id,
                view.snapshot(url = url, progress = 100, isLoading = false, finished = true),
            )
            restoreScroll()
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: WebResourceError,
        ) {
            if (request.isForMainFrame) {
                actions.reportBrowserError(tab.id, error.description.toString())
            }
        }

        override fun onReceivedSslError(
            view: WebView,
            handler: SslErrorHandler,
            error: SslError,
        ) {
            handler.cancel()
            actions.reportBrowserError(tab.id, "Stage blocked an invalid HTTPS certificate")
        }

        override fun onRenderProcessGone(
            view: WebView,
            detail: RenderProcessGoneDetail,
        ): Boolean {
            pool.discard(tab.id, view)
            actions.reportBrowserRendererGone(tab.id, detail.didCrash())
            return true
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            actions.reportBrowserPage(
                tab.id,
                view.snapshot(progress = newProgress, isLoading = newProgress < 100),
            )
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            actions.reportBrowserPage(
                tab.id,
                view.snapshot(title = title.orEmpty(), isLoading = view.progress < 100),
            )
        }

        override fun onShowFileChooser(
            webView: WebView,
            filePathCallback: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams,
        ): Boolean {
            onFileChooser(filePathCallback, fileChooserParams)
            return true
        }

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message,
        ): Boolean {
            if (!isUserGesture) return false
            val handled = AtomicBoolean(false)
            val popup = WebView(view.context)
            popup.webViewClient = object : WebViewClient() {
                private fun open(url: String?) {
                    if (!url.isNullOrBlank() && handled.compareAndSet(false, true)) {
                        actions.newBrowserTab(url)
                        popup.destroy()
                    }
                }

                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    open(request.url.toString())
                    return true
                }

                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                    open(url)
                }
            }
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            transport.webView = popup
            resultMsg.sendToTarget()
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            val allowed = allowedWebResources(request.resources)
            if (allowed.isEmpty()) request.deny()
            else onPermissionPrompt(PendingSitePermission.Media(request))
        }

        override fun onPermissionRequestCanceled(request: PermissionRequest) {
            request.deny()
        }

        override fun onGeolocationPermissionsShowPrompt(
            origin: String,
            callback: GeolocationPermissions.Callback,
        ) {
            onPermissionPrompt(PendingSitePermission.Location(origin, callback))
        }
    }

    webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
        actions.enqueueBrowserDownload(
            BrowserDownloadRequest(
                url = url,
                userAgent = userAgent,
                contentDisposition = contentDisposition,
                mimeType = mimeType,
                contentLength = contentLength,
                referringUrl = webView.url,
            ),
        )
    }
}

private fun WebView.snapshot(
    url: String = this.url.orEmpty(),
    title: String = this.title.orEmpty(),
    progress: Int = this.progress,
    isLoading: Boolean = this.progress < 100,
    finished: Boolean = false,
): BrowserPageSnapshot = BrowserPageSnapshot(
    url = url,
    title = title,
    progress = progress,
    isLoading = isLoading,
    canGoBack = canGoBack(),
    canGoForward = canGoForward(),
    scrollY = scrollY,
    finished = finished,
)

private sealed interface PendingSitePermission {
    data class Media(val request: PermissionRequest) : PendingSitePermission
    data class Location(
        val origin: String,
        val callback: GeolocationPermissions.Callback,
    ) : PendingSitePermission

    fun androidPermissions(): List<String> = when (this) {
        is Media -> androidPermissionsFor(request.resources)
        is Location -> listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    fun grant() {
        when (this) {
            is Media -> request.grant(allowedWebResources(request.resources))
            is Location -> callback.invoke(origin, true, false)
        }
    }

    fun deny() {
        when (this) {
            is Media -> request.deny()
            is Location -> callback.invoke(origin, false, false)
        }
    }
}

private fun allowedWebResources(resources: Array<String>): Array<String> = resources.filter { resource ->
    resource == PermissionRequest.RESOURCE_VIDEO_CAPTURE ||
        resource == PermissionRequest.RESOURCE_AUDIO_CAPTURE
}.toTypedArray()

private fun androidPermissionsFor(resources: Array<String>): List<String> = buildList {
    if (PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources) add(Manifest.permission.CAMERA)
    if (PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources) add(Manifest.permission.RECORD_AUDIO)
}

@Composable
private fun SitePermissionDialog(
    prompt: PendingSitePermission,
    onDeny: () -> Unit,
    onAllow: () -> Unit,
) {
    val origin = when (prompt) {
        is PendingSitePermission.Media -> prompt.request.origin.host ?: prompt.request.origin.toString()
        is PendingSitePermission.Location -> Uri.parse(prompt.origin).host ?: prompt.origin
    }
    val capability = when (prompt) {
        is PendingSitePermission.Media -> {
            val resources = prompt.request.resources
            when {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources &&
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE in resources -> "camera and microphone"
                PermissionRequest.RESOURCE_VIDEO_CAPTURE in resources -> "camera"
                else -> "microphone"
            }
        }

        is PendingSitePermission.Location -> "location"
    }
    Dialog(onDismissRequest = onDeny) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = StagePalette.Window,
            modifier = Modifier
                .width(320.dp)
                .border(1.dp, StagePalette.Hairline, RoundedCornerShape(18.dp)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    text = "Allow $capability?",
                    color = StagePalette.TextPrimary,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = "$origin requested access. Stage asks every time and Android remains in control.",
                    color = StagePalette.TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
                Spacer(Modifier.height(17.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    DialogAction("Deny", onDeny)
                    Spacer(Modifier.width(8.dp))
                    DialogAction("Allow once", onAllow, highlighted = true)
                }
            }
        }
    }
}

@Composable
private fun DialogAction(
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (highlighted) StagePalette.Violet.copy(alpha = 0.28f)
                else Color.White.copy(alpha = 0.04f),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = StagePalette.TextPrimary, fontSize = 11.5.sp)
    }
}
