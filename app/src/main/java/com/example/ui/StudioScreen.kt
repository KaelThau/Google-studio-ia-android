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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.example.model.StudioViewMode
import com.example.ui.components.FloatingModePill
import com.example.ui.components.HelpDialog
import com.example.ui.components.StudioTopBar
import com.example.ui.components.UrlSelectorDialog
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioNavy
import com.example.ui.theme.StudioRed
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
    var pendingDomReport by remember { mutableStateOf<String?>(null) }
    var observedUrl by remember { mutableStateOf(StudioJsInjector.DEFAULT_STUDIO_URL) }
    var currentMode by remember { mutableStateOf(StudioViewMode.ALL) }
    var userAgentProfile by remember { mutableStateOf(UserAgentProfile.FIREFOX_DESKTOP) }
    var zoomPercent by remember { mutableIntStateOf(100) }
    var reloadTrigger by remember { mutableLongStateOf(0L) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showUrlSelector by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Native Android "Save As..." File Picker Launcher for .txt files
    val saveTxtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        val contentToSave = pendingDomReport
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
                    snackbarHostState.showSnackbar("Archivo estructura_aistudio.txt guardado correctamente")
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
                        snackbarHostState.showSnackbar("Extrayendo estructura de la página...")
                    }
                    extractDomTrigger = System.currentTimeMillis()
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
                currentMode = currentMode,
                zoomPercent = zoomPercent,
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
                    pendingDomReport = report
                    // Also copy to clipboard automatically as a bonus backup
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("estructura_aistudio", report))
                    } catch (_: Exception) {}
                    // Open native Android Save File dialog
                    saveTxtLauncher.launch("estructura_aistudio.txt")
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

            // Floating Quick Mode Pill at the bottom
            FloatingModePill(
                currentMode = currentMode,
                onModeSelected = { mode ->
                    currentMode = mode
                    scope.launch {
                        snackbarHostState.showSnackbar("Modo: ${mode.title}")
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .testTag("floating_mode_pill")
            )
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
