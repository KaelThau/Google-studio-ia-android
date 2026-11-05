package com.example.ui.components

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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioBlue
import com.example.ui.theme.StudioCard
import com.example.ui.theme.StudioGreen
import com.example.ui.theme.StudioIndigo
import com.example.ui.theme.StudioTextMuted
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = StudioBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guía de Studio Tablet",
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
                Text(
                    text = "Esta aplicación optimiza la versión completa de Google AI Studio para tablets y pantallas táctiles grandes mediante modos de enfoque inteligente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StudioTextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                SectionTitle("Vistas de Pantalla Completa")

                HelpItem(
                    icon = Icons.Default.Chat,
                    title = "Modo Chat (100%)",
                    description = "Oculta el código y el preview para escribir prompts cómodamente y leer las respuestas con ancho completo.",
                    tint = StudioBlue
                )

                HelpItem(
                    icon = Icons.Default.Code,
                    title = "Modo Código (100%)",
                    description = "Expande el explorador de archivos y el editor al 100% de la pantalla para revisar y editar código sin recortes.",
                    tint = StudioGreen
                )

                HelpItem(
                    icon = Icons.Default.PhoneAndroid,
                    title = "Modo Preview (100%)",
                    description = "Centra y agranda el streaming del emulador o aplicación generada para probarla en grande.",
                    tint = StudioAmber
                )

                HelpItem(
                    icon = Icons.Default.ViewColumn,
                    title = "Modos Split (50/50)",
                    description = "Trabaja con dos columnas simultáneas (Chat + Preview o Código + Preview), ideal para pantallas horizontales de tablets.",
                    tint = StudioIndigo
                )

                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = StudioCard),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = StudioBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Inicio de Sesión Seguro",
                                style = MaterialTheme.typography.labelLarge,
                                color = StudioTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Se utiliza un User-Agent de Chrome de Escritorio y soporte multiventana para que Google permita el inicio de sesión en tu cuenta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = StudioTextMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = StudioBlue,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("help_dialog_dismiss_button")
            ) {
                Text("Entendido")
            }
        }
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = StudioBlue,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun HelpItem(
    icon: ImageVector,
    title: String,
    description: String,
    tint: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
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
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = StudioTextSecondary
            )
        }
    }
}
