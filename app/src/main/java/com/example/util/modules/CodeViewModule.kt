package com.example.util.modules

/**
 * Module 2: Code & Files Full-Screen View (100% Width)
 * Isolated module strictly targeting the exact DOM nodes from estructura_aistudio.txt:
 * - Hides: div.console-left-panel (Chat) and the central ms-console-splitter.
 * - Expands: div.console-right-panel and div.editor-container to 100% width.
 * - Balances: div.tree-view-container (file tree) and div.code-editor (Monaco editor)
 *   so code is wide, readable, and never squished to 61px.
 */
object CodeViewModule {
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

            /* 2. Hide Left Panel (Chat) and Central Splitter */
            ms-console-embed .console-left-panel,
            ms-console-embed .console-left-panel.visible,
            div.console-left-panel,
            ms-console-embed .root > .container > ms-console-splitter {
                display: none !important;
            }

            /* 3. Expand Right Panel to 100% full screen */
            ms-console-embed .console-right-panel,
            ms-console-embed .console-right-panel.visible,
            div.console-right-panel {
                display: flex !important;
                flex-direction: column !important;
                width: 100% !important;
                min-width: 100% !important;
                max-width: 100% !important;
                flex: 1 1 100% !important;
                left: 0 !important;
            }

            /* 4. Expand Subheader and Editor Container to 100% */
            ms-console-embed ms-console-subheader,
            ms-console-embed .editor-container,
            ms-console-embed .editor-container.visible,
            div.editor-container {
                display: flex !important;
                width: 100% !important;
                min-width: 100% !important;
                max-width: 100% !important;
                flex: 1 1 auto !important;
            }

            /* 5. Give File Tree a clean, comfortable sidebar width (28% up to 240px) */
            ms-console-embed .tree-view-container.visible,
            div.tree-view-container.visible {
                display: flex !important;
                width: 28% !important;
                min-width: 160px !important;
                max-width: 240px !important;
                flex: 0 0 auto !important;
            }

            /* 6. Expand Code Editor and Monaco Editor to fill all remaining width */
            ms-console-embed .code-editor,
            div.code-editor,
            ms-console-editor,
            ms-console-editor .editor-workspace,
            ms-console-editor .editor,
            ms-console-editor .monaco-editor {
                display: flex !important;
                flex-direction: column !important;
                width: 100% !important;
                max-width: 100% !important;
                flex: 1 1 0% !important;
                min-width: 0 !important;
            }

            /* 7. Hide Applet Preview Overlay while in Code-Only Mode */
            ms-console-embed .applet-container,
            div.applet-container {
                display: none !important;
            }
        """.trimIndent()
    }
}
