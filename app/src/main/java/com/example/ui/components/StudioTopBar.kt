package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.StudioViewMode
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioGreen
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun StudioTopBar(
    currentMode: StudioViewMode,
    onModeSelected: (StudioViewMode) -> Unit,
    zoomPercent: Int,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onReload: () -> Unit,
    onExtractDom: () -> Unit,
    onExtractErrorReport: () -> Unit,
    onOpenUrlSelector: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMenuExpanded by remember { mutableStateOf(false) }
    var debugMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = StudioSurface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        // statusBarsPadding() ensures the top bar starts strictly below the system clock, notifications, Wi-Fi & battery bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left group: App Icon + View Dropdown + Debug Dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Brand Icon
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Studio Tablet",
                    tint = StudioBlue,
                    modifier = Modifier
                        .padding(start = 2.dp, end = 2.dp)
                        .size(20.dp)
                )

                // 1. View Mode Dropdown Button ("Vista: <Modo>")
                Box {
                    OutlinedButton(
                        onClick = { viewMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StudioBlue.copy(alpha = 0.14f),
                            contentColor = StudioBlue
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBlue.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(start = 10.dp, end = 6.dp, top = 0.dp, bottom = 0.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("dropdown_view_mode_button")
                    ) {
                        Icon(
                            imageVector = modeIcon(currentMode),
                            contentDescription = null,
                            tint = StudioBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentMode.shortLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StudioTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Desplegar Vistas",
                            tint = StudioBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = viewMenuExpanded,
                        onDismissRequest = { viewMenuExpanded = false },
                        modifier = Modifier.background(StudioCard)
                    ) {
                        StudioViewMode.entries.forEach { mode ->
                            val isSelected = mode == currentMode
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) StudioBlue else StudioTextPrimary
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = modeIcon(mode),
                                        contentDescription = null,
                                        tint = if (isSelected) StudioBlue else StudioTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = StudioBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    viewMenuExpanded = false
                                    onModeSelected(mode)
                                },
                                modifier = Modifier.testTag("menu_item_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                // 2. Debug / Analysis Dropdown Button ("Debug ▾")
                Box {
                    OutlinedButton(
                        onClick = { debugMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StudioCard,
                            contentColor = StudioTextPrimary
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioBorder),
                        contentPadding = PaddingValues(start = 10.dp, end = 6.dp, top = 0.dp, bottom = 0.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("dropdown_debug_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = StudioGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Debug",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StudioTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Desplegar Opciones de Debug",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = debugMenuExpanded,
                        onDismissRequest = { debugMenuExpanded = false },
                        modifier = Modifier.background(StudioCard)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Guardar Estructura DOM (.txt)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = null,
                                    tint = StudioGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                debugMenuExpanded = false
                                onExtractDom()
                            },
                            modifier = Modifier.testTag("menu_item_extract_dom")
                        )

                        HorizontalDivider(color = StudioBorder.copy(alpha = 0.5f))

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Capturar Logs y Red (.txt)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = null,
                                    tint = StudioRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                debugMenuExpanded = false
                                onExtractErrorReport()
                            },
                            modifier = Modifier.testTag("menu_item_extract_errors")
                        )
                    }
                }
            }

            // Right group: Zoom Controls + Reload + URL/Section Selector + Help
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Zoom Out
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("button_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Reducir Zoom",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Zoom Percentage Pill (Tap to reset to 100%)
                Text(
                    text = "$zoomPercent%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (zoomPercent != 100) StudioBlue else StudioTextMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onResetZoom() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("zoom_percent_label")
                )

                // Zoom In
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("button_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Aumentar Zoom",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reload Page
                IconButton(
                    onClick = onReload,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("button_reload_page")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Recargar Página",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Navigate / URL & Browser Profile Selector
                IconButton(
                    onClick = onOpenUrlSelector,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("button_open_url_selector")
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Secciones y Perfil de Navegador",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Help Dialog
                IconButton(
                    onClick = onOpenHelp,
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("button_open_help_dialog")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Ayuda y Guía",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun modeIcon(mode: StudioViewMode): ImageVector {
    return when (mode) {
        StudioViewMode.ALL -> Icons.Default.Dashboard
        StudioViewMode.CHAT_ONLY -> Icons.Default.Chat
        StudioViewMode.CODE_ONLY -> Icons.Default.Code
        StudioViewMode.PREVIEW_ONLY -> Icons.Default.PhoneAndroid
        StudioViewMode.SPLIT_CHAT_PREVIEW -> Icons.Default.ViewColumn
        StudioViewMode.SPLIT_CODE_PREVIEW -> Icons.Default.ViewColumn
    }
}
