package com.example.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.example.model.StudioViewMode
import com.example.util.StudioJsInjector

enum class UserAgentProfile(val label: String) {
    AUTO_SMART("Automático Inteligente (Recomendado)"),
    CLEAN_CHROME_ANDROID("Chrome Android Nativo (Limpio)"),
    FIREFOX_DESKTOP("Firefox Desktop (Solo Login)"),
    CHROME_DESKTOP("Chrome Linux Desktop")
}

class StudioJsBridge(
    private val onDomReportReady: (String) -> Unit,
    private val onErrorReportReady: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onDomExtracted(report: String) {
        mainHandler.post {
            onDomReportReady(report)
        }
    }

    @JavascriptInterface
    fun onErrorReportExtracted(report: String) {
        mainHandler.post {
            onErrorReportReady(report)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun StudioWebView(
    targetUrl: String,
    navigateTrigger: Long,
    extractDomTrigger: Long,
    extractErrorTrigger: Long,
    toggleSettingsTrigger: Long,
    currentMode: StudioViewMode,
    zoomPercent: Int,
    codeZoomPercent: Int,
    showSuggestions: Boolean,
    reloadTrigger: Long,
    userAgentProfile: UserAgentProfile,
    onProgressChanged: (Int) -> Unit,
    onPageTitleChanged: (String) -> Unit,
    onErrorOccurred: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onDomExtracted: (String) -> Unit,
    onErrorReportExtracted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnDomExtracted by rememberUpdatedState(onDomExtracted)
    val currentOnErrorReportExtracted by rememberUpdatedState(onErrorReportExtracted)
    val latestMode by rememberUpdatedState(currentMode)
    val latestCodeZoom by rememberUpdatedState(codeZoomPercent)
    val latestShowSuggestions by rememberUpdatedState(showSuggestions)

    val nativeDiagnosticLogs = remember { mutableStateListOf<String>() }

    fun appendNativeLog(entry: String) {
        if (nativeDiagnosticLogs.size > 200) {
            nativeDiagnosticLogs.removeAt(0)
        }
        nativeDiagnosticLogs.add(entry)
    }

    val defaultWebViewUa = remember {
        try {
            WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            StudioJsInjector.DESKTOP_USER_AGENT
        }
    }

    val cleanChromeAndroidUa = remember(defaultWebViewUa) {
        defaultWebViewUa
            .replace("; wv)", ")")
            .replace("; wv;", ";")
            .replace(Regex("Version/\\d+\\.\\d+\\s+"), "")
    }

    val resolvedUserAgent = remember(userAgentProfile, cleanChromeAndroidUa) {
        when (userAgentProfile) {
            UserAgentProfile.AUTO_SMART -> cleanChromeAndroidUa
            UserAgentProfile.CLEAN_CHROME_ANDROID -> cleanChromeAndroidUa
            UserAgentProfile.FIREFOX_DESKTOP -> StudioJsInjector.FIREFOX_USER_AGENT
            UserAgentProfile.CHROME_DESKTOP -> StudioJsInjector.DESKTOP_USER_AGENT
        }
    }

    var isInitialLoadDone by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                mediaPlaybackRequiresUserGesture = false
                allowFileAccess = true
                allowContentAccess = true
                setSupportMultipleWindows(false)
                javaScriptCanOpenWindowsAutomatically = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = resolvedUserAgent
                textZoom = zoomPercent
            }

            addJavascriptInterface(
                StudioJsBridge(
                    onDomReportReady = { report ->
                        currentOnDomExtracted(report)
                    },
                    onErrorReportReady = { jsReport ->
                        val nativeSection = buildString {
                            append("\n\n--- 5. TRAZA NATIVA DEL MOTOR WEBVIEW ANDROID (REQUESTS, HTTP & CONSOLE) ---\n")
                            if (nativeDiagnosticLogs.isEmpty()) {
                                append("  (Sin registros nativos)\n")
                            } else {
                                nativeDiagnosticLogs.forEach { line ->
                                    append("  ").append(line).append("\n")
                                }
                            }
                        }
                        currentOnErrorReportExtracted(jsReport + nativeSection)
                    }
                ),
                "AndroidBridge"
            )

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }
    }

    LaunchedEffect(resolvedUserAgent) {
        if (webView.settings.userAgentString != resolvedUserAgent) {
            webView.settings.userAgentString = resolvedUserAgent
            if (isInitialLoadDone) {
                webView.reload()
            }
        }
    }

    LaunchedEffect(zoomPercent) {
        webView.settings.textZoom = zoomPercent
    }

    LaunchedEffect(currentMode, codeZoomPercent, showSuggestions) {
        val script = StudioJsInjector.getModeScript(currentMode, codeZoomPercent, showSuggestions)
        webView.evaluateJavascript(script, null)
    }

    LaunchedEffect(extractDomTrigger) {
        if (extractDomTrigger > 0L) {
            webView.evaluateJavascript(StudioJsInjector.getDomExtractionScript(), null)
        }
    }

    LaunchedEffect(extractErrorTrigger) {
        if (extractErrorTrigger > 0L) {
            webView.evaluateJavascript(StudioJsInjector.getErrorReportExtractionScript(), null)
        }
    }

    LaunchedEffect(toggleSettingsTrigger) {
        if (toggleSettingsTrigger > 0L) {
            webView.evaluateJavascript(StudioJsInjector.getToggleSettingsSidebarScript(), null)
        }
    }

    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0L) {
            webView.reload()
        }
    }

    LaunchedEffect(targetUrl, navigateTrigger) {
        if (!isInitialLoadDone) {
            isInitialLoadDone = true
            webView.loadUrl(targetUrl)
        } else if (navigateTrigger > 0L) {
            webView.loadUrl(targetUrl)
        }
    }

    DisposableEffect(Unit) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    onUrlChanged(it)
                    if (userAgentProfile == UserAgentProfile.AUTO_SMART && view != null) {
                        val desiredUa = if (it.contains("accounts.google.com")) {
                            StudioJsInjector.FIREFOX_USER_AGENT
                        } else {
                            cleanChromeAndroidUa
                        }
                        if (view.settings.userAgentString != desiredUa) {
                            view.settings.userAgentString = desiredUa
                            appendNativeLog("[UA_AUTO_SWITCH] Switched UA for $it -> $desiredUa")
                        }
                    }
                }
                view?.evaluateJavascript(StudioJsInjector.getLoginStealthScript(), null)
                view?.evaluateJavascript(StudioJsInjector.getErrorLoggerInitScript(), null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onUrlChanged(it) }
                CookieManager.getInstance().flush()
                view?.evaluateJavascript(StudioJsInjector.getLoginStealthScript(), null)
                view?.evaluateJavascript(StudioJsInjector.getErrorLoggerInitScript(), null)
                view?.evaluateJavascript(StudioJsInjector.getInitScript()) {
                    view.evaluateJavascript(
                        StudioJsInjector.getModeScript(latestMode, latestCodeZoom, latestShowSuggestions),
                        null
                    )
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val reqUrl = request?.url?.toString() ?: ""
                val errCode = error?.errorCode ?: -1
                val desc = error?.description?.toString() ?: "Error de red"
                appendNativeLog("[NET_ERR_$errCode] $reqUrl -> $desc")
                if (request?.isForMainFrame == true) {
                    onErrorOccurred(desc)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val statusCode = errorResponse?.statusCode ?: 0
                val reqUrl = request?.url?.toString() ?: ""
                val method = request?.method ?: "GET"
                val reqHeaders = request?.requestHeaders?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: ""
                val respHeaders = errorResponse?.responseHeaders?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: ""
                appendNativeLog(
                    "[HTTP_ERR_$statusCode $method] $reqUrl\n      ReqHeaders: {$reqHeaders}\n      RespHeaders: {$respHeaders}"
                )
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: ""
                if (url.contains("clients6.google.com") ||
                    url.contains("googleapis.com") ||
                    url.contains("MakerSuiteService") ||
                    url.contains("/_/MakerSuite") ||
                    url.contains("applet")
                ) {
                    val headers = request?.requestHeaders?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: ""
                    appendNativeLog("[NATIVE_REQ] ${request?.method} $url | Headers: {$headers}")
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                if (userAgentProfile == UserAgentProfile.AUTO_SMART && view != null) {
                    val desiredUa = if (url.contains("accounts.google.com")) {
                        StudioJsInjector.FIREFOX_USER_AGENT
                    } else {
                        cleanChromeAndroidUa
                    }
                    if (view.settings.userAgentString != desiredUa) {
                        view.settings.userAgentString = desiredUa
                        appendNativeLog("[UA_PRE_NAV_SWITCH] Switched UA before loading $url")
                        view.loadUrl(url)
                        return true
                    }
                }
                return try {
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        false
                    } else {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                        true
                    }
                } catch (e: Exception) {
                    true
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { onPageTitleChanged(it) }
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { cm ->
                    val lvl = cm.messageLevel()?.name ?: "LOG"
                    if (lvl == "ERROR" || lvl == "WARNING") {
                        appendNativeLog("[WEB_CONSOLE_$lvl] ${cm.message()} (${cm.sourceId()}:${cm.lineNumber()})")
                    }
                }
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                if (transport != null && view != null) {
                    val tempWebView = WebView(context)
                    tempWebView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            v: WebView?,
                            req: WebResourceRequest?
                        ): Boolean {
                            val popupUrl = req?.url?.toString()
                            if (!popupUrl.isNullOrBlank()) {
                                view.loadUrl(popupUrl)
                            }
                            tempWebView.destroy()
                            return true
                        }

                        override fun onPageStarted(v: WebView?, popupUrl: String?, favicon: Bitmap?) {
                            if (!popupUrl.isNullOrBlank()) {
                                view.loadUrl(popupUrl)
                            }
                            tempWebView.destroy()
                        }
                    }
                    transport.webView = tempWebView
                    resultMsg.sendToTarget()
                    return true
                }
                return false
            }
        }

        onDispose {
            webView.stopLoading()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}
