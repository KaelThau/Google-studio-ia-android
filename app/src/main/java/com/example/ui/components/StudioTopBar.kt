package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.StudioViewMode
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioGreen
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioNavy
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
    showSuggestions: Boolean,
    onToggleSuggestions: () -> Unit,
    onOpenAiStudioSettings: () -> Unit,
    onReload: () -> Unit,
    onExtractDom: () -> Unit,
    onExtractErrorReport: () -> Unit,
    onOpenUrlSelector: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMenuExpanded by remember { mutableStateOf(false) }
    var toolsMenuExpanded by remember { mutableStateOf(false) }
    var debugMenuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = StudioSurface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: App Icon + 3 Dropdown Menus ([Vista ▾], [Opciones ▾], [Debug ▾])
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Studio Tablet",
                    tint = StudioBlue,
                    modifier = Modifier.size(20.dp)
                )

                // 1. View Mode Dropdown Menu
                Box {
                    OutlinedButton(
                        onClick = { viewMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioBlue.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StudioBlue.copy(alpha = 0.14f),
                            contentColor = StudioTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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
                            color = StudioTextPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Desplegar Vistas",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = viewMenuExpanded,
                        onDismissRequest = { viewMenuExpanded = false },
                        modifier = Modifier.background(StudioCard)
                    ) {
                        StudioViewMode.entries.forEach { mode ->
                            val isSelected = currentMode == mode
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
                                trailingIcon = if (isSelected) {
                                    {
                                        Text(
                                            text = "Seleccionado",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = StudioBlue
                                        )
                                    }
                                } else null,
                                onClick = {
                                    viewMenuExpanded = false
                                    onModeSelected(mode)
                                },
                                modifier = Modifier.testTag("menu_item_mode_${mode.name.lowercase()}")
                            )
                        }
                    }
                }

                // 2. Options Dropdown Menu ([⚙️ Opciones ▾])
                Box {
                    OutlinedButton(
                        onClick = { toolsMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StudioNavy.copy(alpha = 0.6f),
                            contentColor = StudioTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("dropdown_options_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = StudioAmber,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Opciones",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StudioTextPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Desplegar Opciones",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = toolsMenuExpanded,
                        onDismissRequest = { toolsMenuExpanded = false },
                        modifier = Modifier.background(StudioCard)
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Menú Lateral AI Studio (⚙️)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = StudioTextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = StudioBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                toolsMenuExpanded = false
                                onOpenAiStudioSettings()
                            },
                            modifier = Modifier.testTag("menu_item_aistudio_settings")
                        )

                        HorizontalDivider(color = StudioBorder)

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (showSuggestions) "Ocultar Sugerencias del Chat" else "Mostrar Sugerencias del Chat",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = if (showSuggestions) StudioAmber else StudioTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = if (showSuggestions) {
                                {
                                    Text(
                                        text = "Visibles",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StudioAmber
                                    )
                                }
                            } else null,
                            onClick = {
                                toolsMenuExpanded = false
                                onToggleSuggestions()
                            },
                            modifier = Modifier.testTag("menu_item_toggle_suggestions")
                        )

                        HorizontalDivider(color = StudioBorder)

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Recargar Página",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = StudioGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                toolsMenuExpanded = false
                                onReload()
                            },
                            modifier = Modifier.testTag("menu_item_reload_page")
                        )

                        HorizontalDivider(color = StudioBorder)

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Conexión y Secciones (🌐)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = StudioTextPrimary
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = StudioIndigo,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                toolsMenuExpanded = false
                                onOpenUrlSelector()
                            },
                            modifier = Modifier.testTag("menu_item_open_url_selector")
                        )
                    }
                }

                // 3. Debug Dropdown Menu ([🐞 Debug ▾])
                Box {
                    OutlinedButton(
                        onClick = { debugMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = StudioNavy.copy(alpha = 0.6f),
                            contentColor = StudioTextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("dropdown_debug_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = StudioGreen,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Debug",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = StudioTextPrimary
                        )
                        Spacer(modifier = Modifier.width(2.dp))
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
                                    imageVector = Icons.Default.DataObject,
                                    contentDescription = null,
                                    tint = StudioBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                debugMenuExpanded = false
                                onExtractDom()
                            },
                            modifier = Modifier.testTag("menu_item_extract_dom")
                        )

                        HorizontalDivider(color = StudioBorder)

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
                                    tint = StudioAmber,
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

            // Right Group: Zoom Controls + Help
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("button_zoom_out")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomOut,
                        contentDescription = "Reducir Zoom",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "$zoomPercent%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (zoomPercent != 100) StudioBlue else StudioTextMuted,
                    modifier = Modifier
                        .clickable { onResetZoom() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("zoom_percent_label")
                )

                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("button_zoom_in")
                ) {
                    Icon(
                        imageVector = Icons.Default.ZoomIn,
                        contentDescription = "Aumentar Zoom",
                        tint = StudioTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onOpenHelp,
                    modifier = Modifier
                        .size(32.dp)
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
