package com.example.util.modules

/**
 * Module 1: Chat Full-Screen View (100% Width & Height)
 * Isolated module strictly targeting the exact DOM nodes from estructura_aistudio.txt:
 * - Softly collapses the top project header bar (div.page-header) without display:none so the
 *   settings gear button (<ms-settings-toggle-button>) remains alive in the DOM and can open the
 *   settings sidebar (<ms-settings-sidebar>).
 * - Expands: div.console-left-panel, ms-code-assistant-chat, and its internal containers to 100% width and height.
 * - Softly collapses: div.console-right-panel and the central ms-console-splitter.
 * - Softly collapses or shows the bottom suggestion cards depending on showSuggestions,
 *   using strict selectors that NEVER match ms-autoscroll-container or chat history turns.
 */
object ChatViewModule {
    fun getCss(showSuggestions: Boolean = false): String {
        val suggestionsCss = if (!showSuggestions) {
            """
            /* Soft-collapse ONLY the Chat Suggestion Cards Carousel without touching chat history */
            ms-code-assistant-chat [data-studio-suggestions="true"],
            ms-code-assistant-chat ms-prompt-suggestions,
            ms-code-assistant-chat ms-zero-state,
            ms-code-assistant-chat .zero-state-container,
            ms-code-assistant-chat .starter-cards-container,
            ms-code-assistant-chat .suggestions-carousel {
                height: 0px !important;
                min-height: 0px !important;
                max-height: 0px !important;
                margin: 0px !important;
                padding: 0px !important;
                border: none !important;
                overflow: hidden !important;
                opacity: 0 !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }
            """.trimIndent()
        } else {
            ""
        }

        return """
            /* 0. Soft-collapse Top Project Bar (div.page-header) in Chat Mode to free 64px of vertical height
                  while keeping ms-settings-toggle-button alive in the DOM */
            ms-console-embed > .page-header,
            ms-console-embed div.page-header {
                height: 0px !important;
                min-height: 0px !important;
                max-height: 0px !important;
                margin: 0px !important;
                padding: 0px !important;
                border: none !important;
                overflow: hidden !important;
                opacity: 0 !important;
                pointer-events: none !important;
            }

            /* 1. Ensure main root container spans 100% width and full 100% height */
            ms-console-embed .root,
            ms-console-embed .root > .container {
                width: 100% !important;
                max-width: 100% !important;
                height: 100% !important;
                max-height: 100% !important;
                top: 0 !important;
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
                height: 100% !important;
                flex: 1 1 100% !important;
                opacity: 1 !important;
                visibility: visible !important;
                pointer-events: auto !important;
            }

            /* 3. Ensure ms-code-assistant-chat and its inner elements use the full width & height
                  and protect ms-autoscroll-container so virtual scroll NEVER unmounts chat history */
            ms-code-assistant-chat,
            ms-code-assistant-chat > .container,
            ms-code-assistant-chat ms-autoscroll-container,
            ms-code-assistant-chat .bottom-container,
            ms-code-assistant-chat .input-container {
                width: 100% !important;
                max-width: 100% !important;
                box-sizing: border-box !important;
            }

            ms-code-assistant-chat,
            ms-code-assistant-chat > .container {
                display: flex !important;
                flex-direction: column !important;
                height: 100% !important;
                flex: 1 1 100% !important;
            }

            ms-code-assistant-chat ms-autoscroll-container {
                display: block !important;
                flex: 1 1 auto !important;
                min-height: 120px !important;
                opacity: 1 !important;
                visibility: visible !important;
                pointer-events: auto !important;
                overflow-y: auto !important;
            }

            /* 4. Soft-collapse Right Panel (Code + Preview) and Central Splitter without display:none */
            ms-console-embed .console-right-panel,
            ms-console-embed .console-right-panel.visible,
            div.console-right-panel,
            ms-console-embed .root > .container > ms-console-splitter {
                width: 0px !important;
                min-width: 0px !important;
                max-width: 0px !important;
                flex: 0 0 0px !important;
                margin: 0px !important;
                padding: 0px !important;
                border: none !important;
                overflow: hidden !important;
                opacity: 0 !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }

            $suggestionsCss
        """.trimIndent()
    }
}
