package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudioViewMode
import com.example.ui.components.HelpDialog
import com.example.ui.components.StudioTopBar
import com.example.ui.components.UrlSelectorDialog
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioNavy
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.util.StudioJsInjector
import kotlinx.coroutines.launch

@Composable
fun StudioScreen() {
    var targetUrl by remember { mutableStateOf(StudioJsInjector.DEFAULT_STUDIO_URL) }
    var observedUrl by remember { mutableStateOf(StudioJsInjector.DEFAULT_STUDIO_URL) }
    var navigateTrigger by remember { mutableLongStateOf(0L) }
    var extractDomTrigger by remember { mutableLongStateOf(0L) }
    var extractErrorTrigger by remember { mutableLongStateOf(0L) }
    var toggleSettingsTrigger by remember { mutableLongStateOf(0L) }
    var currentMode by remember { mutableStateOf(StudioViewMode.ALL) }
    var zoomPercent by remember { mutableIntStateOf(100) }
    var codeZoomPercent by remember { mutableIntStateOf(100) }
    var showSuggestions by remember { mutableStateOf(false) }
    var userAgentProfile by remember { mutableStateOf(UserAgentProfile.AUTO_SMART) }
    var reloadTrigger by remember { mutableLongStateOf(0L) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showUrlSelector by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var pendingTextToSave by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val saveTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val content = pendingTextToSave
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(content.toByteArray(Charsets.UTF_8))
                }
                scope.launch {
                    snackbarHostState.showSnackbar("Archivo .txt guardado correctamente")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Error al guardar: ${e.message}")
                }
            }
        }
        pendingTextToSave = null
    }

    // Back handler: if in a specific mode, switch back to ALL mode first
    BackHandler(enabled = currentMode != StudioViewMode.ALL) {
        currentMode = StudioViewMode.ALL
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            StudioTopBar(
                currentMode = currentMode,
                onModeSelected = { mode ->
                    currentMode = mode
                    scope.launch {
                        snackbarHostState.showSnackbar("Modo: ${mode.title}")
                    }
                },
                zoomPercent = zoomPercent,
                onZoomIn = {
                    if (zoomPercent < 200) zoomPercent += 10
                },
                onZoomOut = {
                    if (zoomPercent > 50) zoomPercent -= 10
                },
                onResetZoom = {
                    zoomPercent = 100
                },
                showSuggestions = showSuggestions,
                onToggleSuggestions = {
                    showSuggestions = !showSuggestions
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (showSuggestions) "Sugerencias visibles" else "Sugerencias ocultas"
                        )
                    }
                },
                onOpenAiStudioSettings = {
                    toggleSettingsTrigger = System.currentTimeMillis()
                    scope.launch {
                        snackbarHostState.showSnackbar("Abriendo menú lateral de AI Studio...")
                    }
                },
                onExtractDom = {
                    extractDomTrigger = System.currentTimeMillis()
                    scope.launch {
                        snackbarHostState.showSnackbar("Extrayendo estructura DOM...")
                    }
                },
                onExtractErrorReport = {
                    extractErrorTrigger = System.currentTimeMillis()
                    scope.launch {
                        snackbarHostState.showSnackbar("Recopilando logs de consola y red...")
                    }
                },
                onReload = {
                    errorMessage = null
                    reloadTrigger = System.currentTimeMillis()
                },
                onOpenUrlSelector = { showUrlSelector = true },
                onOpenHelp = { showHelpDialog = true }
            )
        },
        containerColor = StudioNavy
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main WebView
            StudioWebView(
                targetUrl = targetUrl,
                navigateTrigger = navigateTrigger,
                extractDomTrigger = extractDomTrigger,
                extractErrorTrigger = extractErrorTrigger,
                toggleSettingsTrigger = toggleSettingsTrigger,
                currentMode = currentMode,
                zoomPercent = zoomPercent,
                codeZoomPercent = codeZoomPercent,
                showSuggestions = showSuggestions,
                userAgentProfile = userAgentProfile,
                reloadTrigger = reloadTrigger,
                onProgressChanged = { progress ->
                    pageProgress = progress
                    if (progress >= 100) {
                        errorMessage = null
                    }
                },
                onPageTitleChanged = { /* Title update */ },
                onErrorOccurred = { error ->
                    errorMessage = error
                },
                onUrlChanged = { newUrl ->
                    observedUrl = newUrl
                },
                onDomExtracted = { domReport ->
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Studio DOM", domReport))
                    } catch (_: Exception) {}
                    pendingTextToSave = domReport
                    saveTxtLauncher.launch("estructura_aistudio.txt")
                },
                onErrorReportExtracted = { errorReport ->
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("AI Studio Error Report", errorReport))
                    } catch (_: Exception) {}
                    pendingTextToSave = errorReport
                    saveTxtLauncher.launch("errores_aistudio.txt")
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("studio_webview")
            )

            // Linear Progress Indicator at the top of the viewport
            if (pageProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = StudioBlue,
                    trackColor = StudioNavy,
                    strokeCap = StrokeCap.Round
                )
            }

            // Floating Code Font Size Overlay Bar (only in Code & Split modes)
            AnimatedVisibility(
                visible = currentMode == StudioViewMode.CODE_ONLY || currentMode == StudioViewMode.SPLIT_CODE_PREVIEW,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Surface(
                    color = StudioSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                        .testTag("code_font_overlay_bar")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "A-",
                            color = StudioTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    if (codeZoomPercent > 50) codeZoomPercent -= 10
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("code_zoom_out_button")
                        )
                        Text(
                            text = "$codeZoomPercent%",
                            color = StudioBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clickable { codeZoomPercent = 100 }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("code_zoom_reset_button")
                        )
                        Text(
                            text = "A+",
                            color = StudioTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    if (codeZoomPercent < 200) codeZoomPercent += 10
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("code_zoom_in_button")
                        )
                    }
                }
            }

            // Error Overlay Card if network error occurred
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(0.85f)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = StudioRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No se pudo cargar Google AI Studio",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage ?: "Verifica tu conexión a Internet y vuelve a intentarlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = StudioTextMuted,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = {
                                errorMessage = null
                                reloadTrigger = System.currentTimeMillis()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StudioBlue,
                                contentColor = StudioNavy
                            ),
                            modifier = Modifier.testTag("error_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Reintentar Carga")
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showUrlSelector) {
        UrlSelectorDialog(
            currentUrl = observedUrl,
            currentUserAgent = userAgentProfile,
            onUserAgentSelected = { profile ->
                userAgentProfile = profile
                scope.launch {
                    snackbarHostState.showSnackbar("Modo de conexión: ${profile.label}")
                }
            },
            onNavigate = { newUrl ->
                errorMessage = null
                targetUrl = newUrl
                navigateTrigger = System.currentTimeMillis()
            },
            onOpenInChrome = { urlToOpen ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                    context.startActivity(intent)
                } catch (_: Exception) {}
            },
            onDismiss = { showUrlSelector = false }
        )
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}
