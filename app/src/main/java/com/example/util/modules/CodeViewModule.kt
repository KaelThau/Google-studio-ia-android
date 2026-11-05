package com.example.util.modules

import java.util.Locale

/**
 * Module 2: Code & Files Full-Screen Vertical Split View (25% Top Tree / 75% Bottom Code)
 * Isolated module strictly targeting the exact DOM nodes from estructura_aistudio.txt:
 * 1. Softly collapses: div.page-header, div.console-left-panel (Chat), central ms-console-splitter,
 *    and ms-console-subheader without display:none so Angular components and settings gear stay alive.
 * 2. Stacks vertically (flex-direction: column):
 *    - Top section (25% height, 100% width): div.tree-view-container (file explorer).
 *    - Bottom section (75% height, 100% width): div.code-editor (Monaco editor).
 * 3. Option 1 Implementation:
 *    - Width compensation + Native Monaco Word Wrap ('wordWrap: on') & dynamic fontSize/lineHeight
 *      so enlarged code automatically wraps to the next line without overflowing off the right edge.
 */
object CodeViewModule {

    fun getCss(codeZoomPercent: Int = 100): String {
        val clampedZoom = codeZoomPercent.coerceIn(60, 240)
        val zoomFactor = String.format(Locale.US, "%.3f", clampedZoom / 100.0)
        val compensatedPct = String.format(Locale.US, "%.3f%%", 10000.0 / clampedZoom)
        val fontSizePx = ((14.0 * clampedZoom) / 100.0).toInt().coerceAtLeast(8)
        val lineHeightPx = (fontSizePx * 1.45).toInt().coerceAtLeast(12)

        return """
            /* 0. Soft-collapse Top Project Bar (div.page-header) in Code Mode to free 64px of vertical height
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

            /* 1. Ensure main root container spans 100% width and height */
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

            /* 2. Soft-collapse Left Panel (Chat), Central Splitter, and the Subheader button row */
            ms-console-embed .console-left-panel,
            ms-console-embed .console-left-panel.visible,
            div.console-left-panel,
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

            ms-console-embed ms-console-subheader {
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

            /* 3. Expand Right Panel to 100% full screen */
            ms-console-embed .console-right-panel,
            ms-console-embed .console-right-panel.visible,
            div.console-right-panel {
                display: flex !important;
                flex-direction: column !important;
                width: 100% !important;
                min-width: 100% !important;
                max-width: 100% !important;
                height: 100% !important;
                flex: 1 1 100% !important;
                left: 0 !important;
                opacity: 1 !important;
                visibility: visible !important;
                pointer-events: auto !important;
            }

            /* 4. Convert Editor Container into a single vertical column (Top: File Tree, Bottom: Code) */
            ms-console-embed .editor-container,
            ms-console-embed .editor-container.visible,
            div.editor-container {
                display: flex !important;
                flex-direction: column !important;
                width: 100% !important;
                min-width: 100% !important;
                max-width: 100% !important;
                height: 100% !important;
                flex: 1 1 100% !important;
                overflow: hidden !important;
                opacity: 1 !important;
                visibility: visible !important;
                pointer-events: auto !important;
            }

            /* 5. Soft-collapse the original vertical splitter inside editor-container */
            ms-console-embed .editor-container > ms-console-splitter {
                height: 0px !important;
                min-height: 0px !important;
                max-height: 0px !important;
                width: 0px !important;
                flex: 0 0 0px !important;
                overflow: hidden !important;
                opacity: 0 !important;
                pointer-events: none !important;
            }

            /* 6. Top Section: File Tree (100% width, 25% height) */
            ms-console-embed .tree-view-container,
            ms-console-embed .tree-view-container.visible,
            div.tree-view-container {
                display: flex !important;
                flex-direction: column !important;
                width: 100% !important;
                min-width: 100% !important;
                max-width: 100% !important;
                height: 25% !important;
                min-height: 60px !important;
                max-height: 25% !important;
                flex: 0 0 25% !important;
                overflow-y: auto !important;
                border-right: none !important;
                border-bottom: 2px solid #2a364f !important;
            }

            ms-console-embed ms-console-file-tree,
            ms-console-embed mat-tree {
                width: 100% !important;
                max-width: 100% !important;
            }

            /* 7. Bottom Section: Code Editor (100% width, fills remaining 75% height) */
            ms-console-embed .code-editor,
            div.code-editor {
                position: relative !important;
                display: flex !important;
                flex-direction: column !important;
                width: 100% !important;
                min-width: 100% !important;
                max-width: 100% !important;
                height: 75% !important;
                flex: 1 1 75% !important;
                min-height: 120px !important;
                overflow: hidden !important;
                box-sizing: border-box !important;
            }

            ms-console-editor {
                display: block !important;
                width: 100% !important;
                max-width: 100% !important;
                height: 100% !important;
                flex: 1 1 100% !important;
                overflow: hidden !important;
            }

            /* 8. Compensated Width when CSS Zoom Fallback is used (when native Monaco API isn't active yet) */
            ms-console-editor:not([data-native-monaco-scaled="true"]) .editor-workspace {
                zoom: $zoomFactor !important;
                width: $compensatedPct !important;
                max-width: $compensatedPct !important;
                height: $compensatedPct !important;
            }

            /* When native Monaco API IS active, keep container at exact 100% so Monaco wraps lines natively */
            ms-console-editor[data-native-monaco-scaled="true"] .editor-workspace,
            ms-console-editor[data-native-monaco-scaled="true"] .editor,
            ms-console-editor[data-native-monaco-scaled="true"] .monaco-editor {
                zoom: 1 !important;
                width: 100% !important;
                max-width: 100% !important;
                height: 100% !important;
            }

            ms-console-editor .monaco-editor .view-lines,
            ms-console-editor .monaco-editor .view-line,
            ms-console-editor .monaco-editor .view-line > span,
            ms-console-editor .monaco-editor .view-line > span > span,
            ms-console-editor .monaco-editor .margin-view-overlays .line-numbers {
                font-size: ${fontSizePx}px !important;
                line-height: ${lineHeightPx}px !important;
            }

            /* 9. Soft-collapse Applet Preview Overlay while in Code-Only Mode */
            ms-console-embed .applet-container,
            div.applet-container {
                width: 0px !important;
                height: 0px !important;
                overflow: hidden !important;
                opacity: 0 !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }
        """.trimIndent()
    }

    /**
     * JavaScript helper for Option 1:
     * Locates the live Monaco Editor instance (via window.monaco or Angular's __ngContext__ on ms-console-editor)
     * and applies native `wordWrap: "on"`, `wrappingStrategy: "advanced"`, `fontSize`, and `lineHeight`,
     * followed by `editor.layout()` so Monaco re-calculates its exact viewport width and wraps long lines
     * automatically without any horizontal overflow.
     */
    fun getMonacoWrapAndScaleScript(enable: Boolean, codeZoomPercent: Int): String {
        val clampedZoom = codeZoomPercent.coerceIn(60, 240)
        val fontSizePx = ((14.0 * clampedZoom) / 100.0).toInt().coerceAtLeast(8)
        val lineHeightPx = (fontSizePx * 1.45).toInt().coerceAtLeast(12)

        if (!enable) {
            return """
                (function() {
                    window.__studioCodeWrapActive = false;
                    if (window.__studioCodeWrapTimer) {
                        clearInterval(window.__studioCodeWrapTimer);
                        window.__studioCodeWrapTimer = null;
                    }
                })();
            """.trimIndent()
        }

        return """
            (function() {
                window.__studioCodeWrapActive = true;
                window.__studioTargetFontPx = $fontSizePx;
                window.__studioTargetLineHeightPx = $lineHeightPx;

                function findMonacoEditors() {
                    var found = [];
                    function addIfEditor(candidate) {
                        if (candidate &&
                            typeof candidate === 'object' &&
                            typeof candidate.updateOptions === 'function' &&
                            typeof candidate.layout === 'function' &&
                            typeof candidate.getModel === 'function') {
                            if (found.indexOf(candidate) === -1) {
                                found.push(candidate);
                            }
                        }
                    }

                    /* Method A: Global window.monaco registry */
                    try {
                        if (window.monaco && window.monaco.editor && typeof window.monaco.editor.getEditors === 'function') {
                            var eds = window.monaco.editor.getEditors();
                            if (eds && eds.length) {
                                for (var i = 0; i < eds.length; i++) {
                                    addIfEditor(eds[i]);
                                }
                            }
                        }
                    } catch (e) {}

                    /* Method B: Inspect Angular Ivy __ngContext__ and expando properties on editor DOM nodes */
                    var hosts = document.querySelectorAll('ms-console-editor, div.code-editor, div.editor-workspace, div.editor, div.monaco-editor');
                    var visited = new Set();

                    function scanObj(obj, depth) {
                        if (!obj || typeof obj !== 'object' || depth > 4 || visited.has(obj)) return;
                        if (obj instanceof Node || obj === window || obj === document) return;
                        visited.add(obj);

                        addIfEditor(obj);

                        if (Array.isArray(obj)) {
                            for (var j = 0; j < obj.length && j < 80; j++) {
                                scanObj(obj[j], depth + 1);
                            }
                            return;
                        }

                        var keys = Object.keys(obj);
                        for (var k = 0; k < keys.length && k < 50; k++) {
                            var key = keys[k];
                            if (key.indexOf('__') === 0 && key.indexOf('__ngContext__') !== 0) continue;
                            try {
                                scanObj(obj[key], depth + 1);
                            } catch (e) {}
                        }
                    }

                    for (var h = 0; h < hosts.length; h++) {
                        var el = hosts[h];
                        try {
                            if (window.ng && typeof window.ng.getComponent === 'function') {
                                var comp = window.ng.getComponent(el);
                                if (comp) scanObj(comp, 0);
                            }
                        } catch (e) {}

                        var propNames = Object.getOwnPropertyNames(el);
                        for (var p = 0; p < propNames.length; p++) {
                            var prop = propNames[p];
                            if (prop.indexOf('__ngContext__') === 0 || prop.indexOf('__') === 0 || prop.indexOf('monaco') !== -1 || prop.indexOf('editor') !== -1) {
                                try {
                                    scanObj(el[prop], 0);
                                } catch (e) {}
                            }
                        }
                    }

                    return found;
                }

                function syncMonacoLayout() {
                    if (!window.__studioCodeWrapActive) return;

                    /* Ensure Code tab is active if Preview was active */
                    var editorContainer = document.querySelector('ms-console-embed .editor-container, div.editor-container');
                    if (editorContainer && !editorContainer.classList.contains('visible')) {
                        var codeToggleBtn = document.querySelector('ms-build-mode-toggle button, [aria-label*="Code"], [aria-label*="code"]');
                        if (codeToggleBtn) codeToggleBtn.click();
                    }

                    var editors = findMonacoEditors();
                    var consoleEditorEl = document.querySelector('ms-console-editor');
                    var codeEditorWrapper = document.querySelector('ms-console-embed .code-editor, div.code-editor');

                    if (editors.length > 0) {
                        if (consoleEditorEl) {
                            consoleEditorEl.setAttribute('data-native-monaco-scaled', 'true');
                        }
                        var targetW = 0;
                        var targetH = 0;
                        if (consoleEditorEl) {
                            var rect = consoleEditorEl.getBoundingClientRect();
                            targetW = Math.floor(rect.width);
                            targetH = Math.floor(rect.height);
                        } else if (codeEditorWrapper) {
                            var wRect = codeEditorWrapper.getBoundingClientRect();
                            targetW = Math.floor(wRect.width);
                            targetH = Math.floor(wRect.height - 48);
                        }

                        for (var i = 0; i < editors.length; i++) {
                            var ed = editors[i];
                            try {
                                ed.updateOptions({
                                    wordWrap: 'on',
                                    wrappingStrategy: 'advanced',
                                    wrappingIndent: 'same',
                                    fontSize: window.__studioTargetFontPx,
                                    lineHeight: window.__studioTargetLineHeightPx,
                                    scrollBeyondLastColumn: 0
                                });
                                if (targetW > 100 && targetH > 60) {
                                    ed.layout({ width: targetW, height: targetH });
                                } else {
                                    ed.layout();
                                }
                            } catch (e) {}
                        }
                    } else {
                        if (consoleEditorEl) {
                            consoleEditorEl.removeAttribute('data-native-monaco-scaled');
                        }
                        window.dispatchEvent(new Event('resize'));
                    }
                }

                syncMonacoLayout();
                setTimeout(syncMonacoLayout, 120);
                setTimeout(syncMonacoLayout, 350);

                if (window.__studioCodeWrapTimer) {
                    clearInterval(window.__studioCodeWrapTimer);
                }
                window.__studioCodeWrapTimer = setInterval(syncMonacoLayout, 800);
            })();
        """.trimIndent()
    }
}
