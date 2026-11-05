package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.StudioViewMode
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioGreen
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
    onReload: () -> Unit,
    onExtractDom: () -> Unit,
    onOpenUrlSelector: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        tonalElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            // First Row: App Branding + Export Structure Button + Zoom & Action Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Title & Brand
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Studio Logo",
                        tint = StudioBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Studio Tablet",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = StudioTextPrimary
                    )
                }

                // Controls: Export DOM (.txt) + Zoom + Reload + URL + Help
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prominent Export DOM Structure Button
                    Button(
                        onClick = onExtractDom,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioGreen,
                            contentColor = StudioNavy
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("button_extract_dom")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Guardar Estructura TXT",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Guardar TXT",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

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

                    // Zoom Label
                    Text(
                        text = "$zoomPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (zoomPercent != 100) StudioBlue else StudioTextMuted,
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
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

                    // Reload
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

                    // Navigate / URL Selector
                    IconButton(
                        onClick = onOpenUrlSelector,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("button_open_url_selector")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Cambiar Sección o URL",
                            tint = StudioTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Help
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

            Spacer(modifier = Modifier.height(4.dp))

            // Second Row: Mode Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VISTA:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextMuted,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(end = 2.dp)
                )

                StudioViewMode.entries.forEach { mode ->
                    val isSelected = currentMode == mode
                    FilterChip(
                        selected = isSelected,
                        onClick = { onModeSelected(mode) },
                        label = {
                            Text(
                                text = mode.shortLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = Color.Transparent,
                            selectedContainerColor = StudioBlue.copy(alpha = 0.18f),
                            labelColor = StudioTextSecondary,
                            selectedLabelColor = StudioBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = StudioBorder,
                            selectedBorderColor = StudioBlue
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("chip_mode_${mode.name.lowercase()}")
                    )
                }
            }
        }
    }
}
