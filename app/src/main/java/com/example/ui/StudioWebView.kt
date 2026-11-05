package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    FIREFOX_DESKTOP("Firefox Desktop (Recomendado para Login)"),
    CLEAN_CHROME_ANDROID("Chrome Nativo del Dispositivo (Sin marca WebView)"),
    CHROME_DESKTOP("Chrome Linux Desktop")
}

private class DomExtractionBridge(
    private val onReportReady: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onDomExtracted(report: String) {
        mainHandler.post {
            onReportReady(report)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun StudioWebView(
    targetUrl: String,
    navigateTrigger: Long,
    extractDomTrigger: Long,
    currentMode: StudioViewMode,
    zoomPercent: Int,
    reloadTrigger: Long,
    userAgentProfile: UserAgentProfile,
    onProgressChanged: (Int) -> Unit,
    onPageTitleChanged: (String) -> Unit,
    onErrorOccurred: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onDomExtracted: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentOnDomExtracted by rememberUpdatedState(onDomExtracted)

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
            UserAgentProfile.FIREFOX_DESKTOP -> StudioJsInjector.FIREFOX_USER_AGENT
            UserAgentProfile.CLEAN_CHROME_ANDROID -> cleanChromeAndroidUa
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
                DomExtractionBridge { report ->
                    currentOnDomExtracted(report)
                },
                "AndroidBridge"
            )

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }
    }

    // Update User-Agent dynamically when profile switches
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

    // React to view mode changes by injecting layout scripts
    LaunchedEffect(currentMode) {
        val script = StudioJsInjector.getModeScript(currentMode)
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

    // React ONLY to explicit user navigation requests (not internal redirects)
    LaunchedEffect(targetUrl, navigateTrigger) {
        if (!isInitialLoadDone || navigateTrigger > 0) {
            isInitialLoadDone = true
            webView.loadUrl(targetUrl)
        }
    }

    DisposableEffect(resolvedUserAgent) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { onUrlChanged(it) }
                view?.evaluateJavascript(StudioJsInjector.getStealthScript(), null)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onUrlChanged(it) }
                CookieManager.getInstance().flush()

                view?.evaluateJavascript(StudioJsInjector.getStealthScript(), null)
                view?.evaluateJavascript(StudioJsInjector.getInitScript()) {
                    view.evaluateJavascript(StudioJsInjector.getModeScript(currentMode), null)
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString() ?: "Error de red"
                    onErrorOccurred(desc)
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
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
