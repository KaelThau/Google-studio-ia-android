package com.example.util.modules

/**
 * Module 1: Chat Full-Screen View (100% Width)
 * Isolated module strictly targeting the exact DOM nodes from estructura_aistudio.txt:
 * - Expands: div.console-left-panel, ms-code-assistant-chat, and its internal containers to 100% width.
 * - Hides: div.console-right-panel and the central ms-console-splitter.
 */
object ChatViewModule {
    fun getCss(): String {
        return """
            /* 1. Ensure main root container spans 100% width */
            ms-console-embed .root,
            ms-console-embed .root > .container {
                width: 100% !important;
                max-width: 100% !important;
                display: flex !important;
                flex-direction: row !important;
            }

            /* 2. Expand Left Panel (Chat) to 100% full screen */
            ms-console-embed .console-left-panel,
            ms-console-embed .console-left-panel.visible,
            div.console-left-panel {
                display: flex !important;
                flex-direction: column !important;
                width: 100% !important;
                min-width: 100% !important;
                max-width: 100% !important;
                flex: 1 1 100% !important;
            }

            /* 3. Ensure ms-code-assistant-chat and its inner elements use the full width */
            ms-code-assistant-chat,
            ms-code-assistant-chat > .container,
            ms-code-assistant-chat ms-autoscroll-container,
            ms-code-assistant-chat .bottom-container,
            ms-code-assistant-chat .input-container {
                width: 100% !important;
                max-width: 100% !important;
                box-sizing: border-box !important;
            }

            /* 4. Hide Right Panel (Code + Preview) and Central Splitter */
            ms-console-embed .console-right-panel,
            ms-console-embed .console-right-panel.visible,
            div.console-right-panel,
            ms-console-embed .root > .container > ms-console-splitter {
                display: none !important;
            }
        """.trimIndent()
    }
}
