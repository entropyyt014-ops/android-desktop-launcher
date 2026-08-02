package com.entropy.stage.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import java.util.LinkedHashMap

internal class BrowserWebViewPool(
    context: Context,
    private val maxLiveViews: Int = 2,
) {
    private val viewContext = context
    private val defaultUserAgent by lazy { WebSettings.getDefaultUserAgent(viewContext) }
    private val views = LinkedHashMap<String, WebView>(4, 0.75f, true)
    private var activeTabId: String? = null

    fun obtain(tab: BrowserTab): WebView {
        views[tab.id]?.let { existing ->
            detachFromParent(existing)
            configureBrowserWebView(existing, tab.profile, defaultUserAgent)
            return existing
        }
        val webView = WebView(viewContext).apply {
            setBackgroundColor(Color.rgb(16, 17, 22))
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            isFocusable = true
            isFocusableInTouchMode = true
        }
        configureBrowserWebView(webView, tab.profile, defaultUserAgent)
        views[tab.id] = webView
        trimToBudget(exceptTabId = tab.id)
        return webView
    }

    fun discard(tabId: String, expected: WebView? = null) {
        val webView = views[tabId] ?: return
        if (expected != null && webView !== expected) return
        views.remove(tabId)
        if (activeTabId == tabId) activeTabId = null
        destroy(webView)
    }

    fun applyProfile(webView: WebView, profile: BrowserProfile) {
        configureBrowserWebView(webView, profile, defaultUserAgent)
    }

    fun createTransient(tab: BrowserTab): WebView {
        views.keys.filterNot { it == tab.id }.toList().forEach(::discard)
        return WebView(viewContext).apply {
            setBackgroundColor(Color.TRANSPARENT)
            visibility = View.INVISIBLE
            configureBrowserWebView(this, tab.profile, defaultUserAgent)
        }
    }

    fun retainTabs(tabIds: Set<String>) {
        views.keys.filterNot(tabIds::contains).forEach { tabId -> discard(tabId) }
    }

    fun pauseAll() {
        views.values.forEach(WebView::onPause)
    }

    fun resumeAll() {
        views.forEach { (tabId, webView) ->
            if (tabId == activeTabId) webView.onResume() else webView.onPause()
        }
    }

    fun setActive(tabId: String) {
        activeTabId = tabId
        resumeAll()
    }

    fun destroyAll() {
        views.values.toList().forEach(::destroy)
        views.clear()
        activeTabId = null
    }

    private fun trimToBudget(exceptTabId: String) {
        while (views.size > maxLiveViews) {
            val oldest = views.entries.firstOrNull { it.key != exceptTabId } ?: return
            views.remove(oldest.key)
            destroy(oldest.value)
        }
    }

    private fun destroy(webView: WebView) {
        detachFromParent(webView)
        webView.stopLoading()
        webView.webChromeClient = null
        webView.webViewClient = android.webkit.WebViewClient()
        webView.removeAllViews()
        webView.destroy()
    }

    private fun detachFromParent(webView: WebView) {
        (webView.parent as? ViewGroup)?.removeView(webView)
    }
}

@SuppressLint("SetJavaScriptEnabled")
internal fun configureBrowserWebView(
    webView: WebView,
    profile: BrowserProfile,
    defaultUserAgent: String,
) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        setGeolocationEnabled(true)
        loadsImagesAutomatically = true
        mediaPlaybackRequiresUserGesture = true
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        allowFileAccess = false
        allowContentAccess = false
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        setSupportMultipleWindows(true)
        javaScriptCanOpenWindowsAutomatically = true
        safeBrowsingEnabled = true
        cacheMode = WebSettings.LOAD_DEFAULT
        textZoom = 100
        when (profile) {
            BrowserProfile.DESKTOP -> {
                userAgentString = desktopUserAgent(defaultUserAgent)
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            BrowserProfile.ADAPTIVE -> {
                userAgentString = defaultUserAgent
                useWideViewPort = true
                loadWithOverviewMode = true
            }

            BrowserProfile.MOBILE -> {
                userAgentString = defaultUserAgent
                useWideViewPort = false
                loadWithOverviewMode = false
            }
        }
    }
    CookieManager.getInstance().apply {
        setAcceptCookie(true)
        setAcceptThirdPartyCookies(webView, true)
    }
}

internal fun desktopUserAgent(defaultUserAgent: String): String {
    val chromeToken = Regex("Chrome/[0-9.]+")
        .find(defaultUserAgent)
        ?.value
        ?: "Chrome/131.0.0.0"
    return "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) $chromeToken Safari/537.36"
}
