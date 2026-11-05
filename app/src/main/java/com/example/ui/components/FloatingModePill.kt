package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.StudioViewMode
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioGreen
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioNavy
import com.example.ui.theme.StudioTextMuted

@Composable
fun FloatingModePill(
    currentMode: StudioViewMode,
    onModeSelected: (StudioViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }

    Box(
        modifier = modifier.padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = StudioNavy.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
            modifier = Modifier.border(1.dp, StudioBorder, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PillButton(
                            icon = Icons.Default.Dashboard,
                            label = "Todo",
                            isSelected = currentMode == StudioViewMode.ALL,
                            tint = StudioBlue,
                            onClick = { onModeSelected(StudioViewMode.ALL) }
                        )

                        PillButton(
                            icon = Icons.Default.Chat,
                            label = "Chat",
                            isSelected = currentMode == StudioViewMode.CHAT_ONLY,
                            tint = StudioBlue,
                            onClick = { onModeSelected(StudioViewMode.CHAT_ONLY) }
                        )

                        PillButton(
                            icon = Icons.Default.Code,
                            label = "Código",
                            isSelected = currentMode == StudioViewMode.CODE_ONLY,
                            tint = StudioGreen,
                            onClick = { onModeSelected(StudioViewMode.CODE_ONLY) }
                        )

                        PillButton(
                            icon = Icons.Default.PhoneAndroid,
                            label = "Preview",
                            isSelected = currentMode == StudioViewMode.PREVIEW_ONLY,
                            tint = StudioAmber,
                            onClick = { onModeSelected(StudioViewMode.PREVIEW_ONLY) }
                        )

                        PillButton(
                            icon = Icons.Default.ViewColumn,
                            label = "Split",
                            isSelected = currentMode == StudioViewMode.SPLIT_CHAT_PREVIEW || currentMode == StudioViewMode.SPLIT_CODE_PREVIEW,
                            tint = StudioIndigo,
                            onClick = {
                                if (currentMode == StudioViewMode.SPLIT_CHAT_PREVIEW) {
                                    onModeSelected(StudioViewMode.SPLIT_CODE_PREVIEW)
                                } else {
                                    onModeSelected(StudioViewMode.SPLIT_CHAT_PREVIEW)
                                }
                            }
                        )
                    }
                }

                // Minimize / Expand Toggle Button
                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("floating_pill_toggle")
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.Tune,
                        contentDescription = if (isExpanded) "Ocultar Acceso Rápido" else "Mostrar Modos",
                        tint = StudioTextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PillButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .background(
                color = if (isSelected) tint.copy(alpha = 0.22f) else Color.Transparent,
                shape = CircleShape
            )
            .testTag("pill_mode_${label.lowercase()}")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) tint else StudioTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
