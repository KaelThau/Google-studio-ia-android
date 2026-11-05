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
     * Used ONLY during accounts.google.com login to bypass WebView login blocks.
     */
    const val FIREFOX_USER_AGENT =
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0"

    const val DEFAULT_STUDIO_URL = "https://aistudio.google.com/"
    const val BUILD_STUDIO_URL = "https://aistudio.google.com/build"
    const val PROMPTS_STUDIO_URL = "https://aistudio.google.com/prompts/new_chat"

    /**
     * Stealth script ONLY applied on accounts.google.com during login.
     */
    fun getLoginStealthScript(): String {
        return """
            (function() {
                try {
                    if (window.location.hostname.includes('accounts.google.com')) {
                        Object.defineProperty(navigator, 'webdriver', { get: () => false });
                        delete window._webview;
                    }
                } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Full Telemetry & Deep Network/Console Debugger.
     * Captures ALL network calls (XHR + fetch, both 200 OK and errors), request headers,
     * request payloads, response headers, response bodies, and all console levels (log/info/warn/error).
     */
    fun getErrorLoggerInitScript(): String {
        return """
            (function() {
                if (window.__studioErrorLoggerInstalled) return;
                window.__studioErrorLoggerInstalled = true;
                window.__studioCapturedErrors = window.__studioCapturedErrors || [];
                window.__studioAllNetworkCalls = window.__studioAllNetworkCalls || [];
                window.__studioAllConsoleLogs = window.__studioAllConsoleLogs || [];

                function pushError(type, detail) {
                    const entry = "[" + new Date().toISOString() + "] [" + type + "] " + detail;
                    window.__studioCapturedErrors.push(entry);
                    if (window.__studioCapturedErrors.length > 200) {
                        window.__studioCapturedErrors.shift();
                    }
                }

                function pushNet(entry) {
                    window.__studioAllNetworkCalls.push("[" + new Date().toISOString() + "] " + entry);
                    if (window.__studioAllNetworkCalls.length > 250) {
                        window.__studioAllNetworkCalls.shift();
                    }
                }

                function pushConsole(level, detail) {
                    window.__studioAllConsoleLogs.push("[" + new Date().toISOString() + "] [" + level + "] " + detail);
                    if (window.__studioAllConsoleLogs.length > 250) {
                        window.__studioAllConsoleLogs.shift();
                    }
                }

                // 1. Capture window errors & unhandled promise rejections
                window.addEventListener('error', function(e) {
                    const msg = (e.message || "") + " at " + (e.filename || "") + ":" + (e.lineno || "");
                    pushError("WINDOW_ERROR", msg);
                    pushConsole("WINDOW_ERROR", msg);
                });

                window.addEventListener('unhandledrejection', function(e) {
                    const reason = e.reason ? (e.reason.stack || e.reason.message || JSON.stringify(e.reason)) : "Unknown rejection";
                    pushError("UNHANDLED_REJECTION", reason);
                    pushConsole("UNHANDLED_REJECTION", reason);
                });

                // 2. Capture ALL console levels (log, info, warn, error, debug)
                ['log', 'info', 'warn', 'error', 'debug'].forEach(level => {
                    const orig = console[level];
                    if (!orig) return;
                    console[level] = function() {
                        try {
                            const msg = Array.from(arguments).map(a => {
                                if (a instanceof Error) return a.message + "\n" + a.stack;
                                if (typeof a === 'object') {
                                    try { return JSON.stringify(a).slice(0, 800); } catch(e) { return String(a); }
                                }
                                return String(a);
                            }).join(" ");
                            pushConsole(level.toUpperCase(), msg);
                            if (level === 'error' || level === 'warn') {
                                pushError("CONSOLE_" + level.toUpperCase(), msg);
                            }
                        } catch(e) {}
                        return orig.apply(console, arguments);
                    };
                });

                // 3. Deep observer on XMLHttpRequest (captures URL, method, request headers, request body, status, response headers, response body)
                const origOpen = XMLHttpRequest.prototype.open;
                const origSetHeader = XMLHttpRequest.prototype.setRequestHeader;
                const origSend = XMLHttpRequest.prototype.send;

                XMLHttpRequest.prototype.open = function(method, url) {
                    this.__studioMethod = method;
                    this.__studioUrl = url;
                    this.__studioReqHeaders = {};
                    return origOpen.apply(this, arguments);
                };

                XMLHttpRequest.prototype.setRequestHeader = function(header, value) {
                    try {
                        if (this.__studioReqHeaders) {
                            // Mask only raw Cookie if present, keep Authorization/X-Goog headers visible for debugging
                            this.__studioReqHeaders[header] = String(value).slice(0, 300);
                        }
                    } catch(e) {}
                    return origSetHeader.apply(this, arguments);
                };

                XMLHttpRequest.prototype.send = function(body) {
                    let reqBodyPreview = "";
                    try {
                        if (typeof body === 'string') {
                            reqBodyPreview = body.slice(0, 1000);
                        } else if (body && body.byteLength !== undefined) {
                            reqBodyPreview = "[Binary/ArrayBuffer " + body.byteLength + " bytes]";
                        } else if (body) {
                            reqBodyPreview = String(body).slice(0, 500);
                        }
                    } catch(e) {}

                    this.addEventListener('loadend', function() {
                        try {
                            const urlStr = String(this.__studioUrl || "");
                            // Skip noisy analytics/doubleclick pings in the main RPC trace unless they are API calls
                            const isAnalytics = urlStr.includes("google-analytics.com") || urlStr.includes("doubleclick.net") || urlStr.includes("/ccm/collect");
                            
                            let respPreview = "";
                            try {
                                respPreview = (typeof this.responseText === 'string') ? this.responseText.slice(0, 1500) : "[Non-text response]";
                            } catch(e) {
                                respPreview = "[ResponseText inaccessible]";
                            }

                            let respHeaders = "";
                            try {
                                respHeaders = (this.getAllResponseHeaders() || "").trim().replace(/\r?\n/g, " | ");
                            } catch(e) {}

                            const reqHdrStr = JSON.stringify(this.__studioReqHeaders || {});
                            const fullEntry = "[XHR " + this.status + "] " + (this.__studioMethod || "REQ") + " " + urlStr +
                                "\n      ReqHeaders: " + reqHdrStr +
                                (reqBodyPreview ? "\n      ReqBody: " + reqBodyPreview : "") +
                                (respHeaders ? "\n      RespHeaders: " + respHeaders : "") +
                                (respPreview ? "\n      RespBody: " + respPreview : "");

                            if (!isAnalytics) {
                                pushNet(fullEntry);
                            }
                            if (this.status >= 400 || (this.status === 0 && !isAnalytics)) {
                                pushError("XHR_FAIL_" + this.status, fullEntry);
                            }
                        } catch(e) {}
                    });
                    return origSend.apply(this, arguments);
                };

                // 4. Deep observer on fetch()
                const origFetch = window.fetch;
                if (origFetch) {
                    window.fetch = async function() {
                        const input = arguments[0];
                        const init = arguments[1] || {};
                        const url = input ? (input.url || String(input)) : "unknown";
                        const method = init.method || (input && input.method) || "GET";
                        const isAnalytics = url.includes("google-analytics.com") || url.includes("doubleclick.net") || url.includes("/ccm/collect") || url.includes("analytics.google.com");

                        let reqHeaders = "";
                        try {
                            if (init.headers) {
                                if (init.headers instanceof Headers) {
                                    const hObj = {};
                                    init.headers.forEach((v, k) => { hObj[k] = v; });
                                    reqHeaders = JSON.stringify(hObj);
                                } else {
                                    reqHeaders = JSON.stringify(init.headers);
                                }
                            }
                        } catch(e) {}

                        let reqBody = "";
                        try {
                            if (typeof init.body === 'string') {
                                reqBody = init.body.slice(0, 800);
                            }
                        } catch(e) {}

                        try {
                            const response = await origFetch.apply(this, arguments);
                            if (!isAnalytics) {
                                let textPreview = "";
                                try {
                                    const clone = response.clone();
                                    const text = await clone.text();
                                    textPreview = text.slice(0, 1200);
                                } catch(e) {}

                                const entry = "[FETCH " + response.status + "] " + method + " " + url +
                                    (reqHeaders ? "\n      ReqHeaders: " + reqHeaders : "") +
                                    (reqBody ? "\n      ReqBody: " + reqBody : "") +
                                    (textPreview ? "\n      RespBody: " + textPreview : "");
                                pushNet(entry);

                                if (!response.ok) {
                                    pushError("FETCH_FAIL_" + response.status, entry);
                                }
                            }
                            return response;
                        } catch (err) {
                            if (!isAnalytics) {
                                const errEntry = "[FETCH_ERR] " + method + " " + url + " -> " + (err ? err.toString() : "Error");
                                pushNet(errEntry);
                                pushError("FETCH_NETWORK_ERR", errEntry);
                            }
                            throw err;
                        }
                    };
                }
            })();
        """.trimIndent()
    }

    /**
     * Extracts complete diagnostic telemetry: environment, storage, DOM errors, all API/RPC network calls, and all console logs.
     */
    fun getErrorReportExtractionScript(): String {
        return """
            (function() {
                try {
                    const lines = [];
                    lines.push("==================================================================");
                    lines.push("GOOGLE AI STUDIO - REPORTE COMPLETO DE DEPURACION (FULL TELEMETRY)");
                    lines.push("==================================================================");
                    lines.push("Fecha: " + new Date().toISOString());
                    lines.push("URL Actual: " + window.location.href);
                    lines.push("User-Agent Activo: " + navigator.userAgent);
                    lines.push("Navigator Platform: " + navigator.platform);
                    lines.push("Navigator Vendor: " + navigator.vendor);
                    try {
                        lines.push("Navigator UA Data: " + (navigator.userAgentData ? JSON.stringify(navigator.userAgentData) : "undefined"));
                    } catch(e) {}
                    lines.push("Cookies Habilitadas: " + navigator.cookieEnabled);
                    lines.push("Nombres de Cookies: " +
                        document.cookie.split(';').map(c => c.split('=')[0].trim()).filter(Boolean).join(', ')
                    );
                    try {
                        lines.push("LocalStorage Keys: " + Object.keys(localStorage).join(', '));
                        lines.push("SessionStorage Keys: " + Object.keys(sessionStorage).join(', '));
                    } catch(e) {}
                    lines.push("");

                    lines.push("--- 1. MENSAJES DE ESTADO Y ERRORES EN EL DOM ---");
                    const errorNodes = document.querySelectorAll('.error, [class*="error"], [role="alert"], mat-snack-bar-container, simple-snack-bar, ms-console-assistant-response, .warning, [class*="permission"]');
                    errorNodes.forEach((el, idx) => {
                        if (idx > 45) return;
                        const txt = (el.innerText || "").trim().replace(/\s+/g, " ");
                        if (txt.length > 0) {
                            const tag = el.tagName.toLowerCase();
                            const cls = (typeof el.className === 'string') ? "." + el.className.trim().split(/\s+/).join('.') : "";
                            lines.push("  * <" + tag + cls + ">: " + txt.slice(0, 400));
                        }
                    });
                    lines.push("");

                    lines.push("--- 2. ERRORES CRITICOS CAPTURADOS (4xx/5xx/Excepciones) ---");
                    const errs = window.__studioCapturedErrors || [];
                    if (errs.length === 0) {
                        lines.push("  (Sin errores criticos)");
                    } else {
                        errs.forEach(item => lines.push("  " + item));
                    }
                    lines.push("");

                    lines.push("--- 3. TRAZA COMPLETA DE LLAMADAS DE RED (XHR / FETCH / RPC) ---");
                    const netCalls = window.__studioAllNetworkCalls || [];
                    if (netCalls.length === 0) {
                        lines.push("  (Sin llamadas de red registradas)");
                    } else {
                        netCalls.forEach(item => lines.push("  " + item));
                    }
                    lines.push("");

                    lines.push("--- 4. TODOS LOS LOGS DE CONSOLA (LOG / INFO / WARN / ERROR / DEBUG) ---");
                    const cLogs = window.__studioAllConsoleLogs || [];
                    if (cLogs.length === 0) {
                        lines.push("  (Sin logs de consola)");
                    } else {
                        cLogs.forEach(item => lines.push("  " + item));
                    }

                    const fullReport = lines.join("\n");
                    if (window.AndroidBridge && window.AndroidBridge.onErrorReportExtracted) {
                        window.AndroidBridge.onErrorReportExtracted(fullReport);
                    }
                    return "OK";
                } catch (err) {
                    if (window.AndroidBridge && window.AndroidBridge.onErrorReportExtracted) {
                        window.AndroidBridge.onErrorReportExtracted("ERROR GENERANDO REPORTE: " + err.toString());
                    }
                    return "ERR";
                }
            })();
        """.trimIndent()
    }

    /**
     * Comprehensive DOM Structure Extractor script.
     */
    fun getDomExtractionScript(): String {
        return """
            (function() {
                try {
                    const lines = [];
                    lines.push("==================================================================");
                    lines.push("GOOGLE AI STUDIO - RADIOGRAFIA COMPLETA DE ESTRUCTURA DOM");
                    lines.push("==================================================================");
                    lines.push("Fecha: " + new Date().toISOString());
                    lines.push("URL Actual: " + window.location.href);
                    lines.push("Titulo: " + document.title);
                    lines.push("Viewport: " + window.innerWidth + "x" + window.innerHeight);
                    lines.push("");

                    lines.push("--- 1. COMPONENTES PERSONALIZADOS (Angular / Custom Tags) ---");
                    const allEls = document.querySelectorAll('*');
                    const customTags = new Map();
                    allEls.forEach(el => {
                        const tag = el.tagName.toLowerCase();
                        if (tag.includes('-')) {
                            const rect = el.getBoundingClientRect();
                            const sizeInfo = Math.round(rect.width) + "x" + Math.round(rect.height) + "@(" + Math.round(rect.left) + "," + Math.round(rect.top) + ")";
                            if (!customTags.has(tag)) {
                                customTags.set(tag, []);
                            }
                            if (customTags.get(tag).length < 5) {
                                const cls = (typeof el.className === 'string' && el.className.trim()) ? "." + el.className.trim().split(/\s+/).join('.') : "";
                                customTags.get(tag).push(sizeInfo + " " + cls);
                            }
                        }
                    });
                    customTags.forEach((instances, tag) => {
                        lines.push("  <" + tag + "> -> " + instances.join(" | "));
                    });
                    lines.push("");

                    lines.push("--- 2. ELEMENTOS CLAVE (Chat Inputs, Editores, Iframes, Pestañas) ---");
                    const landmarks = document.querySelectorAll('textarea, [contenteditable="true"], iframe, .monaco-editor, pre, code, [role="tab"], [role="tablist"], [role="main"], [role="navigation"], nav, aside, header, main');
                    landmarks.forEach((el, idx) => {
                        if (idx > 120) return;
                        const rect = el.getBoundingClientRect();
                        if (rect.width === 0 && rect.height === 0) return;
                        const tag = el.tagName.toLowerCase();
                        const id = el.id ? "#" + el.id : "";
                        const cls = (typeof el.className === 'string' && el.className.trim()) ? "." + el.className.trim().split(/\s+/).slice(0, 4).join('.') : "";
                        const role = el.getAttribute('role') ? "[role=" + el.getAttribute('role') + "]" : "";
                        const aria = el.getAttribute('aria-label') ? "[aria-label='" + el.getAttribute('aria-label') + "']" : "";
                        const placeholder = el.getAttribute('placeholder') ? "[placeholder='" + el.getAttribute('placeholder') + "']" : "";
                        const text = (el.innerText || "").trim().replace(/\s+/g, " ").slice(0, 50);
                        
                        const parents = [];
                        let curr = el.parentElement;
                        let pCount = 0;
                        while (curr && curr !== document.body && pCount < 6) {
                            const pTag = curr.tagName.toLowerCase();
                            const pId = curr.id ? "#" + curr.id : "";
                            const pCls = (typeof curr.className === 'string' && curr.className.trim()) ? "." + curr.className.trim().split(/\s+/).slice(0, 2).join('.') : "";
                            const pRect = curr.getBoundingClientRect();
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
                        const rect = node.getBoundingClientRect ? node.getBoundingClientRect() : { width: 0, height: 0, left: 0, top: 0 };
                        const style = window.getComputedStyle ? window.getComputedStyle(node) : {};
                        
                        const tag = node.tagName ? node.tagName.toLowerCase() : "";
                        if (!tag || tag === 'svg' || tag === 'path' || tag === 'script' || tag === 'style' || tag === 'link' || tag === 'meta') return;
                        if (rect.width < 20 && rect.height < 20 && !tag.includes('-')) return;

                        const indent = "  ".repeat(depth);
                        const id = node.id ? "#" + node.id : "";
                        const cls = (typeof node.className === 'string' && node.className.trim())
                            ? "." + node.className.trim().split(/\s+/).slice(0, 5).join('.')
                            : "";
                        
                        const attrs = [];
                        ['role', 'aria-label', 'data-test-id', 'data-testid', 'name', 'slot'].forEach(attr => {
                            if (node.hasAttribute && node.hasAttribute(attr)) {
                                attrs.push(attr + "='" + node.getAttribute(attr) + "'");
                            }
                        });
                        const attrStr = attrs.length > 0 ? " [" + attrs.join(", ") + "]" : "";
                        const boxStr = "(" + Math.round(rect.width) + "x" + Math.round(rect.height) + " @" + Math.round(rect.left) + "," + Math.round(rect.top) + ")";
                        const layoutStr = "{disp:" + style.display + ", pos:" + style.position + ", flex:" + style.flexDirection + "}";

                        lines.push(indent + "<" + tag + id + cls + ">" + attrStr + " " + boxStr + " " + layoutStr);

                        const children = node.children ? Array.from(node.children) : [];
                        children.forEach(child => dumpTree(child, depth + 1));
                    }

                    dumpTree(document.body, 0);

                    const fullReport = lines.join("\n");
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
                if (window.__studioTabletInitialized) return;
                window.__studioTabletInitialized = true;

                let styleTag = document.getElementById('studio-tablet-overrides');
                if (!styleTag) {
                    styleTag = document.createElement('style');
                    styleTag.id = 'studio-tablet-overrides';
                    document.head.appendChild(styleTag);
                }
            })();
        """.trimIndent()
    }

    /**
     * Dispatches to the independent view module for the selected mode.
     */
    fun getModeScript(mode: StudioViewMode): String {
        val cssRules = when (mode) {
            StudioViewMode.ALL -> OriginalViewModule.getCss()
            StudioViewMode.CHAT_ONLY -> ChatViewModule.getCss()
            StudioViewMode.CODE_ONLY -> CodeViewModule.getCss()
            else -> OriginalViewModule.getCss()
        }

        val cleanCss = cssRules.replace("\n", " ").replace("\"", "\\\"").trim()
        val isCodeMode = mode == StudioViewMode.CODE_ONLY

        return """
            (function() {
                let styleTag = document.getElementById('studio-tablet-overrides');
                if (!styleTag) {
                    styleTag = document.createElement('style');
                    styleTag.id = 'studio-tablet-overrides';
                    document.head.appendChild(styleTag);
                }
                styleTag.textContent = "$cleanCss";

                if ($isCodeMode) {
                    setTimeout(function() {
                        window.dispatchEvent(new Event('resize'));
                    }, 60);
                    setTimeout(function() {
                        window.dispatchEvent(new Event('resize'));
                    }, 250);
                } else {
                    setTimeout(function() {
                        window.dispatchEvent(new Event('resize'));
                    }, 60);
                }
            })();
        """.trimIndent()
    }
}
