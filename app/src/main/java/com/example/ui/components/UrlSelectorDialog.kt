package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.UserAgentProfile
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioGreen
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioNavy
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary
import com.example.util.StudioJsInjector

@Composable
fun UrlSelectorDialog(
    currentUrl: String,
    currentUserAgent: UserAgentProfile,
    onUserAgentSelected: (UserAgentProfile) -> Unit,
    onNavigate: (String) -> Unit,
    onOpenInChrome: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var customUrlInput by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                    tint = StudioBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Conexión y Secciones",
                    style = MaterialTheme.typography.titleLarge,
                    color = StudioTextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = StudioAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Perfil de Navegador (Bypass Login Google)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = StudioAmber
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                UserAgentProfile.entries.forEach { profile ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUserAgentSelected(profile) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentUserAgent == profile,
                            onClick = { onUserAgentSelected(profile) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = StudioBlue,
                                unselectedColor = StudioTextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = profile.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (currentUserAgent == profile) StudioTextPrimary else StudioTextSecondary,
                            fontWeight = if (currentUserAgent == profile) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Secciones de Google AI Studio:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = StudioBlue
                )

                Spacer(modifier = Modifier.height(8.dp))

                QuickUrlOption(
                    icon = Icons.Default.Build,
                    title = "AI Studio Build (Applets)",
                    url = StudioJsInjector.BUILD_STUDIO_URL,
                    tint = StudioBlue,
                    onClick = {
                        onNavigate(StudioJsInjector.BUILD_STUDIO_URL)
                        onDismiss()
                    }
                )

                QuickUrlOption(
                    icon = Icons.Default.Home,
                    title = "AI Studio Inicio",
                    url = StudioJsInjector.DEFAULT_STUDIO_URL,
                    tint = StudioGreen,
                    onClick = {
                        onNavigate(StudioJsInjector.DEFAULT_STUDIO_URL)
                        onDismiss()
                    }
                )

                QuickUrlOption(
                    icon = Icons.Default.Chat,
                    title = "Prompts & Chat Workspace",
                    url = StudioJsInjector.PROMPTS_STUDIO_URL,
                    tint = StudioIndigo,
                    onClick = {
                        onNavigate(StudioJsInjector.PROMPTS_STUDIO_URL)
                        onDismiss()
                    }
                )

                QuickUrlOption(
                    icon = Icons.Default.OpenInBrowser,
                    title = "Abrir en Chrome Externo",
                    url = "Usa tu sesión nativa de Chrome si Google bloquea el WebView",
                    tint = StudioAmber,
                    onClick = {
                        onOpenInChrome(currentUrl)
                        onDismiss()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    label = { Text("URL personalizada o proyecto") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioBlue,
                        unfocusedBorderColor = StudioCard,
                        focusedTextColor = StudioTextPrimary,
                        unfocusedTextColor = StudioTextPrimary,
                        focusedLabelColor = StudioBlue,
                        unfocusedLabelColor = StudioTextMuted
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_url_input_field")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (customUrlInput.isNotBlank()) {
                        val validUrl = if (customUrlInput.startsWith("http://") || customUrlInput.startsWith("https://")) {
                            customUrlInput.trim()
                        } else {
                            "https://${customUrlInput.trim()}"
                        }
                        onNavigate(validUrl)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioBlue,
                    contentColor = StudioNavy
                ),
                modifier = Modifier.testTag("url_dialog_confirm_button")
            ) {
                Text("Ir a URL")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("url_dialog_cancel_button")
            ) {
                Text("Cerrar", color = StudioTextSecondary)
            }
        }
    )
}

@Composable
private fun QuickUrlOption(
    icon: ImageVector,
    title: String,
    url: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = StudioCard),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag("quick_url_${title.replace(" ", "_").lowercase()}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = StudioTextPrimary
                )
                Text(
                    text = url,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextMuted,
                    maxLines = 1
                )
            }
        }
    }
}
