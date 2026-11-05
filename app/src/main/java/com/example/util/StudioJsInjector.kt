package com.example.util

import com.example.model.StudioViewMode
import com.example.util.modules.ChatViewModule
import com.example.util.modules.CodeViewModule
import com.example.util.modules.OriginalViewModule

object StudioJsInjector {

    /**
     * Desktop Chrome User Agent on Linux X11 x86_64.
     */
    const val DESKTOP_USER_AGENT =
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

    /**
     * Firefox Desktop User Agent.
     * Bypasses Blink-specific WebView/Chrome Client Hint mismatches on Google accounts login.
     */
    const val FIREFOX_USER_AGENT =
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0"

    const val DEFAULT_STUDIO_URL = "https://aistudio.google.com/"
    const val BUILD_STUDIO_URL = "https://aistudio.google.com/build"
    const val PROMPTS_STUDIO_URL = "https://aistudio.google.com/prompts/new_chat"

    /**
     * Stealth script injected on page start to mask Android WebView properties
     * before Google's anti-embedded-webview scripts execute, and convert
     * window.open / target="_blank" to same-window navigation so login never hangs.
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
     * Initializes in-page error, network (fetch/XHR), and console telemetry capture
     * for the Debug menu's "Capturar Logs y Red (.txt)" action.
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
     * Generates and sends the complete Error, Console, and Network diagnostic report to AndroidBridge.
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
     * Comprehensive DOM Structure Extractor script for "Guardar Estructura DOM (.txt)".
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
     */
    fun getInitScript(): String {
        return """
            (function() {
                if (!document.getElementById('studio-tablet-overrides')) {
                    var styleTag = document.createElement('style');
                    styleTag.id = 'studio-tablet-overrides';
                    document.head.appendChild(styleTag);
                }
            })();
        """.trimIndent()
    }

    /**
     * Generates CSS rules and interactive setup for the active view mode,
     * delegating to the isolated view modules:
     * - OriginalViewModule (ALL)
     * - ChatViewModule (CHAT_ONLY)
     * - CodeViewModule (CODE_ONLY)
     */
    fun getModeScript(mode: StudioViewMode, codeZoomPercent: Int = 100): String {
        val clampedZoom = codeZoomPercent.coerceIn(60, 240)
        val fontSizePx = ((14.0 * clampedZoom) / 100.0).toInt().coerceAtLeast(8)
        val lineHeightPx = (fontSizePx * 1.45).toInt().coerceAtLeast(12)

        val cssRules = when (mode) {
            StudioViewMode.ALL -> OriginalViewModule.getCss()
            StudioViewMode.CHAT_ONLY -> ChatViewModule.getCss()
            StudioViewMode.CODE_ONLY -> CodeViewModule.getCss(clampedZoom)
            StudioViewMode.PREVIEW_ONLY -> """
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
                    display: none !important;
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
                }
            """.trimIndent()
            StudioViewMode.SPLIT_CHAT_PREVIEW -> """
                ms-console-embed .editor-container,
                div.editor-container {
                    display: none !important;
                }
                ms-console-embed .console-left-panel,
                div.console-left-panel {
                    display: flex !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
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
                }
            """.trimIndent()
            StudioViewMode.SPLIT_CODE_PREVIEW -> """
                ms-console-embed .console-left-panel,
                div.console-left-panel,
                ms-console-embed .root > .container > ms-console-splitter {
                    display: none !important;
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

        val cleanCss = cssRules
            .replace(Regex("/\\*[\\s\\S]*?\\*/"), " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .replace("\"", "\\\"")
            .trim()

        val shouldEnsureCodeTab = (mode == StudioViewMode.CODE_ONLY || mode == StudioViewMode.SPLIT_CODE_PREVIEW)
        val monacoWrapScript = CodeViewModule.getMonacoWrapAndScaleScript(shouldEnsureCodeTab, clampedZoom)

        return """
            (function() {
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
