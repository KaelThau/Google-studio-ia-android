package com.example.util

import com.example.model.StudioViewMode
import com.example.util.modules.ChatViewModule
import com.example.util.modules.CodeViewModule
import com.example.util.modules.OriginalViewModule

object StudioJsInjector {

    /**
     * Desktop Chrome User Agent on Linux X11 x86_64.
     * Prevents Google 403 disallowed_useragent and forces the full desktop workspace.
     */
    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    /**
     * Firefox Desktop User Agent for OAuth Login Bypass.
     */
    const val FIREFOX_USER_AGENT =
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0"

    const val DEFAULT_STUDIO_URL = "https://aistudio.google.com/"
    const val BUILD_STUDIO_URL = "https://aistudio.google.com/build"
    const val PROMPTS_STUDIO_URL = "https://aistudio.google.com/prompts/new_chat"

    /**
     * Stealth script injected to bypass WebView detection during Google Login.
     */
    fun getLoginStealthScript(): String {
        return """
            (function() {
                try {
                    Object.defineProperty(navigator, 'webdriver', { get: function() { return false; } });
                    delete window._webview;

                    window.open = function(url) {
                        if (url) {
                            window.location.href = url;
                        }
                        return window;
                    };

                    document.addEventListener('click', function(e) {
                        var link = e.target && e.target.closest ? e.target.closest('a[target="_blank"]') : null;
                        if (link && link.href) {
                            link.removeAttribute('target');
                        }
                    }, true);
                } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Triggers the AI Studio Settings Gear button (<ms-settings-toggle-button>)
     * to open or close the lateral settings drawer (<ms-settings-sidebar>),
     * even when div.page-header is softly collapsed.
     */
    fun getToggleSettingsSidebarScript(): String {
        return """
            (function() {
                try {
                    var gearBtn = document.querySelector(
                        'ms-settings-toggle-button button, ' +
                        'ms-settings-toggle-button [role="button"], ' +
                        '.page-header [aria-label*="Configuraci"], ' +
                        '.page-header [aria-label*="Setting"], ' +
                        '.page-header [aria-label*="setting"], ' +
                        'ms-settings-toggle-button'
                    );
                    if (gearBtn) {
                        gearBtn.click();
                        var evt = new MouseEvent('click', { bubbles: true, cancelable: true, view: window });
                        gearBtn.dispatchEvent(evt);
                        return "OK";
                    }
                    return "NOT_FOUND";
                } catch (e) {
                    return "ERR:" + e.toString();
                }
            })();
        """.trimIndent()
    }

    /**
     * Installs console & network error interceptors for the Debug Report.
     */
    fun getErrorLoggerInitScript(): String {
        return """
            (function() {
                if (window.__studioErrorLoggerInstalled) return;
                window.__studioErrorLoggerInstalled = true;
                window.__studioCapturedLogs = window.__studioCapturedLogs || [];
                window.__studioNetworkErrors = window.__studioNetworkErrors || [];

                function pushLog(level, args) {
                    try {
                        var msg = Array.prototype.slice.call(args).map(function(a) {
                            if (a instanceof Error) return a.toString() + ' ' + (a.stack || '');
                            if (typeof a === 'object') {
                                try { return JSON.stringify(a).slice(0, 400); } catch (e) { return String(a); }
                            }
                            return String(a);
                        }).join(' ');
                        window.__studioCapturedLogs.push('[' + new Date().toISOString() + '] [' + level + '] ' + msg);
                        if (window.__studioCapturedLogs.length > 250) {
                            window.__studioCapturedLogs.shift();
                        }
                    } catch (e) {}
                }

                var origError = console.error;
                var origWarn = console.warn;
                console.error = function() {
                    pushLog('CONSOLE_ERROR', arguments);
                    if (origError) origError.apply(console, arguments);
                };
                console.warn = function() {
                    pushLog('CONSOLE_WARN', arguments);
                    if (origWarn) origWarn.apply(console, arguments);
                };

                window.addEventListener('error', function(e) {
                    pushLog('UNCAUGHT_ERROR', [e.message, e.filename, e.lineno, e.colno]);
                });

                window.addEventListener('unhandledrejection', function(e) {
                    pushLog('UNHANDLED_PROMISE', [e.reason]);
                });

                if (window.fetch) {
                    var origFetch = window.fetch;
                    window.fetch = function() {
                        var url = arguments[0] && arguments[0].url ? arguments[0].url : String(arguments[0]);
                        return origFetch.apply(this, arguments).then(function(resp) {
                            if (!resp.ok) {
                                window.__studioNetworkErrors.push('[' + new Date().toISOString() + '] [FETCH_HTTP_' + resp.status + '] ' + url);
                            }
                            return resp;
                        }).catch(function(err) {
                            window.__studioNetworkErrors.push('[' + new Date().toISOString() + '] [FETCH_FAIL] ' + url + ' -> ' + String(err));
                            throw err;
                        });
                    };
                }
            })();
        """.trimIndent()
    }

    /**
     * Extracts the full Error & Network Report and sends it to AndroidBridge.onErrorReportExtracted.
     */
    fun getErrorReportExtractionScript(): String {
        return """
            (function() {
                try {
                    var lines = [];
                    lines.push("==================================================================");
                    lines.push("GOOGLE AI STUDIO - REPORTE DE DIAGNOSTICO, LOGS Y RED");
                    lines.push("==================================================================");
                    lines.push("Fecha: " + new Date().toISOString());
                    lines.push("URL Actual: " + window.location.href);
                    lines.push("User-Agent: " + navigator.userAgent);
                    lines.push("Viewport: " + window.innerWidth + "x" + window.innerHeight);
                    lines.push("");

                    lines.push("--- 1. ERRORES VISIBLES EN INTERFAZ (Toasts / Banners / Dialogs) ---");
                    var errNodes = document.querySelectorAll('[role="alert"], .mat-mdc-snack-bar-label, .error, [class*="error"], [class*="banner"]');
                    var foundUiErr = 0;
                    errNodes.forEach(function(n) {
                        var txt = (n.innerText || "").trim().replace(/\s+/g, " ");
                        if (txt.length > 0 && txt.length < 500) {
                            lines.push("  * <" + n.tagName.toLowerCase() + "." + (n.className || "") + ">: " + txt);
                            foundUiErr++;
                        }
                    });
                    if (foundUiErr === 0) lines.push("  (Sin alertas visibles en el DOM)");
                    lines.push("");

                    lines.push("--- 2. ERRORES DE RED EN PAGINA (FETCH / XHR) ---");
                    var netErrs = window.__studioNetworkErrors || [];
                    if (netErrs.length === 0) {
                        lines.push("  (Sin errores de fetch capturados)");
                    } else {
                        netErrs.forEach(function(item) { lines.push("  " + item); });
                    }
                    lines.push("");

                    lines.push("--- 3. CONSOLA JAVASCRIPT (ERROR / WARN / UNCAUGHT) ---");
                    var jsLogs = window.__studioCapturedLogs || [];
                    if (jsLogs.length === 0) {
                        lines.push("  (Sin errores de consola capturados)");
                    } else {
                        jsLogs.forEach(function(item) { lines.push("  " + item); });
                    }

                    var report = lines.join("\n");
                    if (window.AndroidBridge && window.AndroidBridge.onErrorReportExtracted) {
                        window.AndroidBridge.onErrorReportExtracted(report);
                    }
                    return "OK";
                } catch (e) {
                    if (window.AndroidBridge && window.AndroidBridge.onErrorReportExtracted) {
                        window.AndroidBridge.onErrorReportExtracted("ERROR GENERANDO REPORTE: " + e.toString());
                    }
                    return "ERR";
                }
            })();
        """.trimIndent()
    }

    /**
     * Extracts the full DOM structure report and sends it to AndroidBridge.onDomExtracted.
     */
    fun getDomExtractionScript(): String {
        return """
            (function() {
                try {
                    var lines = [];
                    lines.push("==================================================================");
                    lines.push("GOOGLE AI STUDIO - RADIOGRAFIA COMPLETA DE ESTRUCTURA DOM");
                    lines.push("==================================================================");
                    lines.push("Fecha: " + new Date().toISOString());
                    lines.push("URL Actual: " + window.location.href);
                    lines.push("Titulo: " + document.title);
                    lines.push("Viewport: " + window.innerWidth + "x" + window.innerHeight);
                    lines.push("");

                    lines.push("--- 1. COMPONENTES PERSONALIZADOS (Angular / Custom Tags) ---");
                    var allEls = document.querySelectorAll('*');
                    var customTags = new Map();
                    allEls.forEach(function(el) {
                        var tag = el.tagName.toLowerCase();
                        if (tag.indexOf('-') !== -1) {
                            var rect = el.getBoundingClientRect();
                            var sizeInfo = Math.round(rect.width) + "x" + Math.round(rect.height) + "@(" + Math.round(rect.left) + "," + Math.round(rect.top) + ")";
                            if (!customTags.has(tag)) {
                                customTags.set(tag, []);
                            }
                            if (customTags.get(tag).length < 5) {
                                var cls = (typeof el.className === 'string' && el.className.trim()) ? "." + el.className.trim().split(/\s+/).join('.') : "";
                                customTags.get(tag).push(sizeInfo + " " + cls);
                            }
                        }
                    });
                    customTags.forEach(function(instances, tag) {
                        lines.push("  <" + tag + "> -> " + instances.join(" | "));
                    });
                    lines.push("");

                    lines.push("--- 2. ELEMENTOS CLAVE (Chat Inputs, Editores, Iframes, Pestañas) ---");
                    var landmarks = document.querySelectorAll('textarea, [contenteditable="true"], iframe, .monaco-editor, pre, code, [role="tab"], [role="tablist"], [role="main"], [role="navigation"], nav, aside, header, main');
                    landmarks.forEach(function(el, idx) {
                        if (idx > 120) return;
                        var rect = el.getBoundingClientRect();
                        if (rect.width === 0 && rect.height === 0) return;
                        var tag = el.tagName.toLowerCase();
                        var id = el.id ? "#" + el.id : "";
                        var cls = (typeof el.className === 'string' && el.className.trim()) ? "." + el.className.trim().split(/\s+/).slice(0, 4).join('.') : "";
                        var role = el.getAttribute('role') ? "[role=" + el.getAttribute('role') + "]" : "";
                        var aria = el.getAttribute('aria-label') ? "[aria-label='" + el.getAttribute('aria-label') + "']" : "";
                        var placeholder = el.getAttribute('placeholder') ? "[placeholder='" + el.getAttribute('placeholder') + "']" : "";
                        var text = (el.innerText || "").trim().replace(/\s+/g, " ").slice(0, 50);

                        var parents = [];
                        var curr = el.parentElement;
                        var pCount = 0;
                        while (curr && curr !== document.body && pCount < 6) {
                            var pTag = curr.tagName.toLowerCase();
                            var pId = curr.id ? "#" + curr.id : "";
                            var pCls = (typeof curr.className === 'string' && curr.className.trim()) ? "." + curr.className.trim().split(/\s+/).slice(0, 2).join('.') : "";
                            var pRect = curr.getBoundingClientRect();
                            parents.push(pTag + pId + pCls + "(" + Math.round(pRect.width) + "x" + Math.round(pRect.height) + ")");
                            curr = curr.parentElement;
                            pCount++;
                        }

                        lines.push("  * " + tag + id + cls + role + aria + placeholder + " [" + Math.round(rect.width) + "x" + Math.round(rect.height) + " @ " + Math.round(rect.left) + "," + Math.round(rect.top) + "] txt='" + text + "'");
                        lines.push("      Padres: " + parents.join(" <- "));
                    });
                    lines.push("");

                    lines.push("--- 3. ARBOL JERARQUICO DE LAYOUT (Containers & Paneles) ---");
                    function dumpTree(node, depth) {
                        if (!node || depth > 14) return;
                        var rect = node.getBoundingClientRect ? node.getBoundingClientRect() : { width: 0, height: 0, left: 0, top: 0 };
                        var style = window.getComputedStyle ? window.getComputedStyle(node) : {};

                        var tag = node.tagName ? node.tagName.toLowerCase() : "";
                        if (!tag || tag === 'svg' || tag === 'path' || tag === 'script' || tag === 'style' || tag === 'link' || tag === 'meta') return;
                        if (rect.width < 20 && rect.height < 20 && tag.indexOf('-') === -1) return;

                        var indent = "  ".repeat(depth);
                        var id = node.id ? "#" + node.id : "";
                        var cls = (typeof node.className === 'string' && node.className.trim())
                            ? "." + node.className.trim().split(/\s+/).slice(0, 5).join('.')
                            : "";

                        var attrs = [];
                        ['role', 'aria-label', 'data-test-id', 'data-testid', 'name', 'slot'].forEach(function(attr) {
                            if (node.hasAttribute && node.hasAttribute(attr)) {
                                attrs.push(attr + "='" + node.getAttribute(attr) + "'");
                            }
                        });
                        var attrStr = attrs.length > 0 ? " [" + attrs.join(", ") + "]" : "";
                        var boxStr = "(" + Math.round(rect.width) + "x" + Math.round(rect.height) + " @" + Math.round(rect.left) + "," + Math.round(rect.top) + ")";
                        var layoutStr = "{disp:" + style.display + ", pos:" + style.position + ", flex:" + style.flexDirection + "}";

                        lines.push(indent + "<" + tag + id + cls + ">" + attrStr + " " + boxStr + " " + layoutStr);

                        var children = node.children ? Array.from(node.children) : [];
                        children.forEach(function(child) { dumpTree(child, depth + 1); });
                    }

                    dumpTree(document.body, 0);

                    var fullReport = lines.join("\n");
                    if (window.AndroidBridge && window.AndroidBridge.onDomExtracted) {
                        window.AndroidBridge.onDomExtracted(fullReport);
                    }
                    return "OK";
                } catch (err) {
                    if (window.AndroidBridge && window.AndroidBridge.onDomExtracted) {
                        window.AndroidBridge.onDomExtracted("ERROR EXTRAER DOM: " + err.toString() + "\n" + (err.stack || ""));
                    }
                    return "ERR";
                }
            })();
        """.trimIndent()
    }

    /**
     * Initial setup script injected once DOM is ready.
     * Safely identifies and tags suggestion cards above the chat input box, while strictly
     * protecting ms-autoscroll-container and all chat message turns from ever being tagged.
     */
    fun getInitScript(): String {
        return """
            (function() {
                if (!document.getElementById('studio-tablet-overrides')) {
                    var styleTag = document.createElement('style');
                    styleTag.id = 'studio-tablet-overrides';
                    document.head.appendChild(styleTag);
                }

                window.__tagChatSuggestions = function() {
                    try {
                        var chatRoot = document.querySelector('ms-code-assistant-chat');
                        if (!chatRoot) return;

                        /* Helper: Never tag the message history viewport or any parent/child of chat turns */
                        function isProtectedChatHistory(el) {
                            if (!el) return true;
                            var tag = (el.tagName || '').toLowerCase();
                            if (tag === 'ms-autoscroll-container' ||
                                tag === 'ms-console-assistant-response' ||
                                tag === 'ms-console-turn' ||
                                tag === 'ms-cmark-node' ||
                                tag === 'ms-chat-step' ||
                                tag === 'ms-expandable-turn') {
                                return true;
                            }
                            if (el.classList && (
                                el.classList.contains('container') ||
                                el.classList.contains('bottom-container') ||
                                el.classList.contains('input-container') ||
                                el.classList.contains('sticky-content')
                            )) {
                                return true;
                            }
                            if (el.querySelector && (
                                el.querySelector('textarea') ||
                                el.querySelector('ms-autoscroll-container') ||
                                el.querySelector('ms-console-assistant-response') ||
                                el.querySelector('ms-console-turn')
                            )) {
                                return true;
                            }
                            return false;
                        }

                        /* First clear any accidental tags on protected elements */
                        var previouslyTagged = chatRoot.querySelectorAll('[data-studio-suggestions="true"]');
                        for (var p = 0; p < previouslyTagged.length; p++) {
                            if (isProtectedChatHistory(previouslyTagged[p])) {
                                previouslyTagged[p].removeAttribute('data-studio-suggestions');
                            }
                        }

                        /* 1. Tag by specific suggestion tag name or class name keywords */
                        var candidates = chatRoot.querySelectorAll('*');
                        for (var i = 0; i < candidates.length; i++) {
                            var el = candidates[i];
                            if (isProtectedChatHistory(el)) continue;
                            /* Also skip if inside a chat turn / response */
                            if (el.closest && el.closest('ms-console-assistant-response, ms-console-turn, ms-chat-step')) continue;

                            var tag = (el.tagName || '').toLowerCase();
                            var cls = (typeof el.className === 'string') ? el.className.toLowerCase() : '';
                            if (tag.indexOf('suggest') !== -1 ||
                                tag.indexOf('starter') !== -1 ||
                                tag.indexOf('zero-state') !== -1 ||
                                tag.indexOf('chip-carousel') !== -1 ||
                                cls.indexOf('suggestion') !== -1 ||
                                cls.indexOf('starter-card') !== -1 ||
                                cls.indexOf('follow-up') !== -1 ||
                                cls.indexOf('followup') !== -1) {
                                el.setAttribute('data-studio-suggestions', 'true');
                            }
                        }

                        /* 2. Tag horizontal card carousels sitting above the chat input box */
                        var containers = chatRoot.querySelectorAll('div, section, ul, ms-horizontal-scroll');
                        for (var j = 0; j < containers.length; j++) {
                            var c = containers[j];
                            if (c.hasAttribute('data-studio-suggestions')) continue;
                            if (isProtectedChatHistory(c)) continue;
                            if (c.closest && c.closest('ms-console-assistant-response, ms-console-turn, ms-chat-step')) continue;

                            var children = c.children;
                            if (!children || children.length < 2 || children.length > 10) continue;

                            var cardCount = 0;
                            var firstTop = null;
                            for (var k = 0; k < children.length; k++) {
                                var ch = children[k];
                                if (isProtectedChatHistory(ch)) continue;
                                var r = ch.getBoundingClientRect();
                                if (r.height >= 64 && r.height <= 240 && r.width >= 110 && r.width <= 360) {
                                    if (firstTop === null) {
                                        firstTop = r.top;
                                        cardCount++;
                                    } else if (Math.abs(r.top - firstTop) < 20) {
                                        cardCount++;
                                    }
                                }
                            }
                            if (cardCount >= 2) {
                                c.setAttribute('data-studio-suggestions', 'true');
                                if (c.parentElement &&
                                    c.parentElement !== chatRoot &&
                                    c.parentElement.children.length === 1 &&
                                    !isProtectedChatHistory(c.parentElement)) {
                                    c.parentElement.setAttribute('data-studio-suggestions', 'true');
                                }
                            }
                        }
                    } catch (e) {}
                };

                window.__tagChatSuggestions();
                if (!window.__studioSuggestionInterval) {
                    window.__studioSuggestionInterval = setInterval(window.__tagChatSuggestions, 700);
                }
            })();
        """.trimIndent()
    }

    /**
     * Generates CSS rules for the active view mode, code zoom, and suggestion visibility.
     * Uses soft non-destructive collapse (height:0 / width:0 / opacity:0 / pointer-events:none)
     * instead of display:none so that:
     * 1. The top project bar (div.page-header) and its settings gear (<ms-settings-toggle-button>)
     *    remain alive in the DOM and can open the lateral settings drawer (<ms-settings-sidebar>).
     * 2. The chat history (<ms-autoscroll-container>) never collapses to 0px or unmounts.
     */
    fun getModeScript(
        mode: StudioViewMode,
        codeZoomPercent: Int = 100,
        showSuggestions: Boolean = false
    ): String {
        val clampedZoom = codeZoomPercent.coerceIn(60, 240)

        val globalSuggestionHideCss = if (!showSuggestions) {
            """
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

        /* Ensure lateral drawers and overlays (Settings sidebar, Features sidebar, CDK menus)
           always appear above everything when opened via the gear button */
        val drawersProtectionCss = """
            ms-settings-sidebar,
            ms-features-sidebar,
            ms-sliding-drawer,
            ms-console-editor-settings,
            .cdk-overlay-container {
                z-index: 99999 !important;
            }
        """.trimIndent()

        val modeCss = when (mode) {
            StudioViewMode.ALL -> OriginalViewModule.getCss()
            StudioViewMode.CHAT_ONLY -> ChatViewModule.getCss(showSuggestions)
            StudioViewMode.CODE_ONLY -> CodeViewModule.getCss(clampedZoom)
            StudioViewMode.PREVIEW_ONLY -> """
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
                ms-console-embed .root,
                ms-console-embed .root > .container {
                    width: 100% !important;
                    max-width: 100% !important;
                    height: 100% !important;
                    display: flex !important;
                }
                ms-console-embed .console-left-panel,
                div.console-left-panel,
                ms-console-embed .root > .container > ms-console-splitter,
                ms-console-embed .editor-container,
                div.editor-container {
                    width: 0px !important;
                    min-width: 0px !important;
                    max-width: 0px !important;
                    flex: 0 0 0px !important;
                    overflow: hidden !important;
                    opacity: 0 !important;
                    visibility: hidden !important;
                    pointer-events: none !important;
                }
                ms-console-embed .console-right-panel,
                div.console-right-panel,
                ms-console-embed .applet-container,
                div.applet-container,
                ms-applet-viewer {
                    display: flex !important;
                    position: relative !important;
                    left: 0 !important;
                    top: 0 !important;
                    width: 100% !important;
                    max-width: 100% !important;
                    height: 100% !important;
                    flex: 1 1 100% !important;
                    opacity: 1 !important;
                    visibility: visible !important;
                    pointer-events: auto !important;
                }
            """.trimIndent()
            StudioViewMode.SPLIT_CHAT_PREVIEW -> """
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
                ms-console-embed .editor-container,
                div.editor-container {
                    width: 0px !important;
                    min-width: 0px !important;
                    max-width: 0px !important;
                    flex: 0 0 0px !important;
                    overflow: hidden !important;
                    opacity: 0 !important;
                    visibility: hidden !important;
                    pointer-events: none !important;
                }
                ms-console-embed .console-left-panel,
                div.console-left-panel {
                    display: flex !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                    opacity: 1 !important;
                    visibility: visible !important;
                    pointer-events: auto !important;
                }
                ms-console-embed .console-right-panel,
                div.console-right-panel,
                ms-console-embed .applet-container,
                div.applet-container {
                    display: flex !important;
                    position: relative !important;
                    left: 0 !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                    opacity: 1 !important;
                    visibility: visible !important;
                    pointer-events: auto !important;
                }
            """.trimIndent()
            StudioViewMode.SPLIT_CODE_PREVIEW -> """
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
                ms-console-embed .console-left-panel,
                div.console-left-panel,
                ms-console-embed .root > .container > ms-console-splitter {
                    width: 0px !important;
                    min-width: 0px !important;
                    max-width: 0px !important;
                    flex: 0 0 0px !important;
                    overflow: hidden !important;
                    opacity: 0 !important;
                    visibility: hidden !important;
                    pointer-events: none !important;
                }
                ms-console-embed .console-right-panel,
                div.console-right-panel {
                    display: flex !important;
                    flex-direction: row !important;
                    width: 100% !important;
                    max-width: 100% !important;
                    flex: 1 1 100% !important;
                }
                ms-console-embed .editor-container,
                div.editor-container {
                    display: flex !important;
                    flex-direction: column !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                }
                ms-console-embed .applet-container,
                div.applet-container {
                    display: flex !important;
                    position: relative !important;
                    left: 0 !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                }
            """.trimIndent()
        }

        val combinedCss = "$drawersProtectionCss\n$modeCss\n$globalSuggestionHideCss"
        val cleanCss = combinedCss
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\"", "\\\"")
            .trim()

        val shouldEnsureCodeTab = (mode == StudioViewMode.CODE_ONLY || mode == StudioViewMode.SPLIT_CODE_PREVIEW)
        val monacoWrapScript = CodeViewModule.getMonacoWrapAndScaleScript(shouldEnsureCodeTab, clampedZoom)

        return """
            (function() {
                if (window.__tagChatSuggestions) {
                    window.__tagChatSuggestions();
                }
                var styleTag = document.getElementById('studio-tablet-overrides');
                if (!styleTag) {
                    styleTag = document.createElement('style');
                    styleTag.id = 'studio-tablet-overrides';
                    document.head.appendChild(styleTag);
                }
                styleTag.textContent = "$cleanCss";
                $monacoWrapScript
                setTimeout(function() { window.dispatchEvent(new Event('resize')); }, 80);
            })();
        """.trimIndent()
    }
}
