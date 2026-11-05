package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
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
    AUTO_SMART("Automático Inteligente (Firefox en Login -> Chrome en AI Studio)"),
    CLEAN_CHROME_ANDROID("Chrome Nativo del Dispositivo (Sin marca WebView)"),
    FIREFOX_DESKTOP("Firefox Desktop (Solo Login)"),
    CHROME_DESKTOP("Chrome Linux Desktop")
}

private class StudioJsBridge(
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

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun StudioWebView(
    targetUrl: String,
    navigateTrigger: Long,
    extractDomTrigger: Long,
    extractErrorTrigger: Long,
    currentMode: StudioViewMode,
    zoomPercent: Int,
    codeZoomPercent: Int,
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

    // Native WebView HTTP/Console/Request trace buffer
    val nativeDiagnosticLogs = remember { mutableStateListOf<String>() }

    fun appendNativeLog(entry: String) {
        nativeDiagnosticLogs.add(entry)
        if (nativeDiagnosticLogs.size > 300) {
            nativeDiagnosticLogs.removeAt(0)
        }
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
            // AUTO_SMART starts with clean Chrome inside AI Studio, and switches to Firefox only if redirected to accounts.google.com
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
                    onDomReportReady = { report -> currentOnDomExtracted(report) },
                    onErrorReportReady = { jsReport ->
                        val combined = buildString {
                            append(jsReport)
                            append("\n\n--- 5. TRAZA NATIVA DEL MOTOR WEBVIEW ANDROID (REQUESTS, HTTP & CONSOLE) ---\n")
                            if (nativeDiagnosticLogs.isEmpty()) {
                                append("  (Sin registros nativos)\n")
                            } else {
                                nativeDiagnosticLogs.forEach { line ->
                                    append("  ").append(line).append("\n")
                                }
                            }
                        }
                        currentOnErrorReportExtracted(combined)
                    }
                ),
                "AndroidBridge"
            )

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }
    }

    // Update User-Agent dynamically when profile switches manually
    LaunchedEffect(resolvedUserAgent) {
        if (webView.settings.userAgentString != resolvedUserAgent) {
            webView.settings.userAgentString = resolvedUserAgent
            webView.reload()
        }
    }

    // React to zoom level changes
    LaunchedEffect(zoomPercent) {
        webView.settings.textZoom = zoomPercent
    }

    // React to view mode or code font zoom changes by injecting layout + font CSS
    LaunchedEffect(currentMode, codeZoomPercent) {
        val script = StudioJsInjector.getModeScript(currentMode, codeZoomPercent)
        webView.evaluateJavascript(script, null)
    }

    // React to manual reload requests
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            webView.reload()
        }
    }

    // React to DOM extraction requests
    LaunchedEffect(extractDomTrigger) {
        if (extractDomTrigger > 0) {
            webView.evaluateJavascript(StudioJsInjector.getDomExtractionScript(), null)
        }
    }

    // React to Error Report extraction requests
    LaunchedEffect(extractErrorTrigger) {
        if (extractErrorTrigger > 0) {
            webView.evaluateJavascript(StudioJsInjector.getErrorReportExtractionScript(), null)
        }
    }

    // React ONLY to explicit user navigation requests
    LaunchedEffect(targetUrl, navigateTrigger) {
        if (!isInitialLoadDone || navigateTrigger > 0) {
            isInitialLoadDone = true
            webView.loadUrl(targetUrl)
        }
    }

    DisposableEffect(userAgentProfile, cleanChromeAndroidUa) {
        webView.webViewClient = object : WebViewClient() {

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request?.let { req ->
                    val reqUrl = req.url?.toString() ?: ""
                    val host = req.url?.host ?: ""
                    // Log all API / RPC / MakerSuite / GoogleAPIs requests at the native level with their headers
                    if (host.contains("clients6.google.com") ||
                        host.contains("googleapis.com") ||
                        reqUrl.contains("MakerSuiteService") ||
                        reqUrl.contains("/_/MakerSuite") ||
                        reqUrl.contains("applet")
                    ) {
                        val hdrs = req.requestHeaders?.entries?.joinToString("; ") { "${it.key}=${it.value.take(80)}" } ?: ""
                        appendNativeLog("[NATIVE_REQ] ${req.method} $reqUrl | Headers: {$hdrs}")
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { currentUrlStr ->
                    onUrlChanged(currentUrlStr)

                    // Smart Auto-Switching between Login (Firefox UA) and AI Studio (Clean Chrome UA)
                    if (userAgentProfile == UserAgentProfile.AUTO_SMART && view != null) {
                        val isGoogleLogin = currentUrlStr.contains("accounts.google.com")
                        val desiredUa = if (isGoogleLogin) {
                            StudioJsInjector.FIREFOX_USER_AGENT
                        } else {
                            cleanChromeAndroidUa
                        }
                        if (view.settings.userAgentString != desiredUa) {
                            view.settings.userAgentString = desiredUa
                            appendNativeLog("[UA_AUTO_SWITCH] Switched UA for ${Uri.parse(currentUrlStr).host} -> $desiredUa")
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

                view?.evaluateJavascript(StudioJsInjector.getErrorLoggerInitScript(), null)
                view?.evaluateJavascript(StudioJsInjector.getInitScript()) {
                    view.evaluateJavascript(StudioJsInjector.getModeScript(latestMode, latestCodeZoom), null)
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                val status = errorResponse?.statusCode ?: 0
                val reqUrl = request?.url?.toString() ?: ""
                val method = request?.method ?: "GET"
                val reason = errorResponse?.reasonPhrase ?: ""
                val reqHeaders = request?.requestHeaders?.entries?.joinToString("; ") { "${it.key}=${it.value.take(80)}" } ?: ""
                val respHeaders = errorResponse?.responseHeaders?.entries?.joinToString("; ") { "${it.key}=${it.value}" } ?: ""
                appendNativeLog("[HTTP_ERR_$status $reason] $method $reqUrl\n      ReqHeaders: {$reqHeaders}\n      RespHeaders: {$respHeaders}")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                val reqUrl = request?.url?.toString() ?: ""
                val desc = error?.description?.toString() ?: "Error de red"
                val code = error?.errorCode ?: 0
                appendNativeLog("[NET_ERR_$code] $reqUrl -> $desc")
                if (request?.isForMainFrame == true) {
                    onErrorOccurred(desc)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false

                // If AUTO_SMART is enabled and navigating to accounts.google.com vs aistudio.google.com,
                // set the matching User-Agent right before the navigation proceeds
                if (userAgentProfile == UserAgentProfile.AUTO_SMART && view != null) {
                    val isGoogleLogin = url.contains("accounts.google.com")
                    val desiredUa = if (isGoogleLogin) {
                        StudioJsInjector.FIREFOX_USER_AGENT
                    } else {
                        cleanChromeAndroidUa
                    }
                    if (view.settings.userAgentString != desiredUa) {
                        view.settings.userAgentString = desiredUa
                        appendNativeLog("[UA_PRE_NAV_SWITCH] Switched UA before loading ${Uri.parse(url).host}")
                    }
                }

                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                return try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                    true
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
                consoleMessage?.let { msg ->
                    appendNativeLog("[WEB_CONSOLE_${msg.messageLevel()}] ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})")
                }
                return super.onConsoleMessage(consoleMessage)
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
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
