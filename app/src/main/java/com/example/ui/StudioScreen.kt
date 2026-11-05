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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun StudioScreen() {
    val context = LocalContext.current
    var targetUrl by remember { mutableStateOf(StudioJsInjector.DEFAULT_STUDIO_URL) }
    var navigateTrigger by remember { mutableLongStateOf(0L) }
    var extractDomTrigger by remember { mutableLongStateOf(0L) }
    var extractErrorTrigger by remember { mutableLongStateOf(0L) }
    var pendingTextToSave by remember { mutableStateOf<String?>(null) }
    var observedUrl by remember { mutableStateOf(StudioJsInjector.DEFAULT_STUDIO_URL) }
    var currentMode by remember { mutableStateOf(StudioViewMode.ALL) }
    var userAgentProfile by remember { mutableStateOf(UserAgentProfile.AUTO_SMART) }
    var zoomPercent by remember { mutableIntStateOf(100) }
    var codeZoomPercent by remember { mutableIntStateOf(100) }
    var reloadTrigger by remember { mutableLongStateOf(0L) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showUrlSelector by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Native Android "Save As..." File Picker Launcher for .txt files (both DOM and Error reports)
    val saveTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val contentToSave = pendingTextToSave
        if (uri != null && contentToSave != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(contentToSave.toByteArray(Charsets.UTF_8))
                            outputStream.flush()
                        }
                        true
                    } catch (e: Exception) {
                        false
                    }
                }
                if (success) {
                    snackbarHostState.showSnackbar("Archivo .txt guardado correctamente")
                } else {
                    snackbarHostState.showSnackbar("Error al guardar el archivo .txt")
                }
            }
        }
    }

    // Back handler: if in a specific mode, switch back to ALL mode first
    BackHandler(enabled = currentMode != StudioViewMode.ALL) {
        currentMode = StudioViewMode.ALL
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // Automatically resizes the layout when the soft keyboard opens or closes
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
                onReload = {
                    errorMessage = null
                    reloadTrigger = System.currentTimeMillis()
                },
                onExtractDom = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Extrayendo estructura DOM...")
                    }
                    extractDomTrigger = System.currentTimeMillis()
                },
                onExtractErrorReport = {
                    scope.launch {
                        snackbarHostState.showSnackbar("Capturando diagnóstico de errores...")
                    }
                    extractErrorTrigger = System.currentTimeMillis()
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
            // 1. Base Layer: WebView
            StudioWebView(
                targetUrl = targetUrl,
                navigateTrigger = navigateTrigger,
                extractDomTrigger = extractDomTrigger,
                extractErrorTrigger = extractErrorTrigger,
                currentMode = currentMode,
                zoomPercent = zoomPercent,
                codeZoomPercent = codeZoomPercent,
                reloadTrigger = reloadTrigger,
                userAgentProfile = userAgentProfile,
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
                onDomExtracted = { report ->
                    pendingTextToSave = report
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("estructura_aistudio", report))
                    } catch (_: Exception) {}
                    saveTxtLauncher.launch("estructura_aistudio.txt")
                },
                onErrorReportExtracted = { errorReport ->
                    pendingTextToSave = errorReport
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("errores_aistudio", errorReport))
                    } catch (_: Exception) {}
                    saveTxtLauncher.launch("errores_aistudio.txt")
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("studio_webview")
            )

            // 2. Top-Layer Native Android Overlay: Code Font Size Controls (Bottom-Right Corner)
            // Rendered natively above the WebView so no CSS or CSP inside the web page can ever hide it.
            if (currentMode == StudioViewMode.CODE_ONLY || currentMode == StudioViewMode.SPLIT_CODE_PREVIEW) {
                Surface(
                    color = StudioSurface.copy(alpha = 0.96f),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, StudioBlue),
                    shadowElevation = 12.dp,
                    tonalElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 18.dp)
                        .zIndex(100f)
                        .testTag("code_font_overlay_bar")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (codeZoomPercent > 60) {
                                    codeZoomPercent -= 10
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = StudioCard,
                                contentColor = StudioBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("code_font_decrease_button")
                        ) {
                            Text(
                                text = "A-",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = StudioBlue
                            )
                        }

                        Text(
                            text = "$codeZoomPercent%",
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .widthIn(min = 44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { codeZoomPercent = 100 }
                                .padding(horizontal = 4.dp, vertical = 6.dp)
                                .testTag("code_font_percent_label")
                        )

                        OutlinedButton(
                            onClick = {
                                if (codeZoomPercent < 240) {
                                    codeZoomPercent += 10
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, StudioBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = StudioCard,
                                contentColor = StudioBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("code_font_increase_button")
                        ) {
                            Text(
                                text = "A+",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = StudioBlue
                            )
                        }
                    }
                }
            }

            // 3. Linear Progress Indicator at the top of the viewport
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

            // 4. Error Overlay Card if network error occurred
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
                    snackbarHostState.showSnackbar("Perfil cambiado: ${profile.label}")
                }
            },
            onNavigate = { newUrl ->
                errorMessage = null
                targetUrl = newUrl
                navigateTrigger = System.currentTimeMillis()
            },
            onOpenInChrome = { urlToOpen ->
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                } catch (e: Exception) {
                    scope.launch {
                        snackbarHostState.showSnackbar("No se pudo abrir el navegador externo")
                    }
                }
            },
            onDismiss = { showUrlSelector = false }
        )
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }
}
