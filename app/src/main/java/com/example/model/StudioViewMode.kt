package com.example.model

/**
 * Modes for displaying Google AI Studio on tablets and large screens.
 */
enum class StudioViewMode(
    val title: String,
    val shortLabel: String,
    val description: String
) {
    ALL(
        title = "Todo / Original",
        shortLabel = "Todo",
        description = "Muestra la interfaz original completa de Google AI Studio con todos los paneles activos."
    ),
    CHAT_ONLY(
        title = "Chat Completo",
        shortLabel = "Chat",
        description = "Maximiza el área de chat y prompts al 100% de la pantalla para redacción cómoda en tablet."
    ),
    CODE_ONLY(
        title = "Código Completo",
        shortLabel = "Código",
        description = "Maximiza el explorador de archivos y el editor de código al 100% del ancho para máxima legibilidad."
    ),
    PREVIEW_ONLY(
        title = "Preview Completo",
        shortLabel = "Preview",
        description = "Centra y maximiza el emulador de streaming o lienzo de previsualización al 100% de la pantalla."
    ),
    SPLIT_CHAT_PREVIEW(
        title = "Chat + Preview",
        shortLabel = "Chat+Prev",
        description = "Divide la pantalla en dos columnas: Chat a la izquierda y Preview en vivo a la derecha."
    ),
    SPLIT_CODE_PREVIEW(
        title = "Código + Preview",
        shortLabel = "Cód+Prev",
        description = "Divide la pantalla en dos columnas: Editor de código a la izquierda y Preview en vivo a la derecha."
    )
}
