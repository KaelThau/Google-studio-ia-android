package com.example.util

import com.example.model.StudioViewMode

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
    fun getStealthScript(): String {
        return """
            (function() {
                try {
                    Object.defineProperty(navigator, 'webdriver', { get: () => false });
                    Object.defineProperty(navigator, 'platform', { get: () => 'Linux x86_64' });
                    delete window._webview;

                    window.open = function(url) {
                        if (url) {
                            window.location.href = url;
                        }
                        return window;
                    };

                    document.addEventListener('click', function(e) {
                        const link = e.target && e.target.closest ? e.target.closest('a[target="_blank"]') : null;
                        if (link && link.href) {
                            link.removeAttribute('target');
                        }
                    }, true);
                } catch (e) {}
            })();
        """.trimIndent()
    }

    /**
     * Comprehensive DOM Structure Extractor script.
     * Captures the live page URL, viewport size, custom Angular components (<ms-*>, <mat-*>, etc.),
     * key interactive landmarks (textareas, editors, iframes), and the full layout tree
     * with bounding boxes and CSS layout properties.
     * Sends the resulting text report to AndroidBridge.onDomExtracted(report).
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

                    // 1. Custom Web / Angular Elements (<ms-*>, <app-*>, <mat-*>, etc.)
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

                    // 2. Key Landmarks (Inputs, Textareas, Editors, Iframes, Buttons, Tabs)
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
                        
                        // Build parent chain (up to 6 levels)
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

                    // 3. Full Hierarchical Layout Tree (Depth up to 14, skipping tiny invisible nodes)
                    lines.push("--- 3. ARBOL JERARQUICO DE LAYOUT (Containers & Paneles) ---");
                    function dumpTree(node, depth) {
                        if (!node || depth > 14) return;
                        const rect = node.getBoundingClientRect ? node.getBoundingClientRect() : { width: 0, height: 0, left: 0, top: 0 };
                        const style = window.getComputedStyle ? window.getComputedStyle(node) : {};
                        
                        // Skip completely hidden or tiny icon internals unless it's a custom tag
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

                window.scanAndTagColumns = function() {
                    try {
                        document.querySelectorAll('a[target="_blank"]').forEach(a => {
                            a.removeAttribute('target');
                        });

                        const containers = document.querySelectorAll(
                            'main, [role="main"], .workspace-container, .builder-container, div[style*="display: flex"], div[style*="flex-direction: row"]'
                        );
                        
                        let bestContainer = null;
                        let maxChildren = 0;

                        containers.forEach(c => {
                            const directChildren = Array.from(c.children).filter(child => {
                                const style = window.getComputedStyle(child);
                                return style.display !== 'none' && child.offsetWidth > 60;
                            });
                            if (directChildren.length >= 2 && directChildren.length > maxChildren) {
                                maxChildren = directChildren.length;
                                bestContainer = c;
                            }
                        });

                        if (bestContainer) {
                            bestContainer.setAttribute('data-ais-workspace', 'true');
                            const children = Array.from(bestContainer.children);
                            
                            children.forEach((child, index) => {
                                child.setAttribute('data-ais-col-index', index.toString());
                                
                                const hasInput = child.querySelector('textarea, [contenteditable="true"]');
                                const hasCode = child.querySelector('.monaco-editor, pre, code, [data-testid*="file"], [data-testid*="code"]');
                                const hasPreview = child.querySelector('iframe, [data-testid*="preview"], canvas, video');

                                if (hasPreview) {
                                    child.setAttribute('data-ais-role', 'preview');
                                } else if (hasCode) {
                                    child.setAttribute('data-ais-role', 'code');
                                } else if (hasInput || index === 0) {
                                    child.setAttribute('data-ais-role', 'chat');
                                } else if (index === children.length - 1) {
                                    child.setAttribute('data-ais-role', 'preview');
                                } else {
                                    child.setAttribute('data-ais-role', 'code');
                                }
                            });
                        }
                    } catch(e) {
                        console.error('scanAndTagColumns error', e);
                    }
                };

                const observer = new MutationObserver(() => {
                    window.scanAndTagColumns();
                });
                observer.observe(document.body, { childList: true, subtree: true });

                window.scanAndTagColumns();
                setTimeout(window.scanAndTagColumns, 1500);
                setTimeout(window.scanAndTagColumns, 3500);
            })();
        """.trimIndent()
    }

    /**
     * Generates CSS rules for the active view mode.
     */
    fun getModeScript(mode: StudioViewMode): String {
        val cssRules = when (mode) {
            StudioViewMode.ALL -> ""
            StudioViewMode.CHAT_ONLY -> """
                [data-ais-workspace="true"] > [data-ais-role="chat"],
                [data-ais-workspace="true"] > [data-ais-col-index="0"] {
                    display: flex !important;
                    width: 100% !important;
                    max-width: 100% !important;
                    flex: 1 1 100% !important;
                    min-width: 100% !important;
                }
                [data-ais-workspace="true"] > [data-ais-role="code"],
                [data-ais-workspace="true"] > [data-ais-col-index="1"],
                [data-ais-workspace="true"] > [data-ais-role="preview"],
                [data-ais-workspace="true"] > [data-ais-col-index="2"] {
                    display: none !important;
                }
            """
            StudioViewMode.CODE_ONLY -> """
                [data-ais-workspace="true"] > [data-ais-role="code"],
                [data-ais-workspace="true"] > [data-ais-col-index="1"] {
                    display: flex !important;
                    width: 100% !important;
                    max-width: 100% !important;
                    flex: 1 1 100% !important;
                    min-width: 100% !important;
                }
                [data-ais-workspace="true"] > [data-ais-role="chat"],
                [data-ais-workspace="true"] > [data-ais-col-index="0"],
                [data-ais-workspace="true"] > [data-ais-role="preview"],
                [data-ais-workspace="true"] > [data-ais-col-index="2"] {
                    display: none !important;
                }
            """
            StudioViewMode.PREVIEW_ONLY -> """
                [data-ais-workspace="true"] > [data-ais-role="preview"],
                [data-ais-workspace="true"] > [data-ais-col-index="2"] {
                    display: flex !important;
                    width: 100% !important;
                    max-width: 100% !important;
                    flex: 1 1 100% !important;
                    min-width: 100% !important;
                    align-items: center !important;
                    justify-content: center !important;
                }
                [data-ais-workspace="true"] > [data-ais-role="chat"],
                [data-ais-workspace="true"] > [data-ais-col-index="0"],
                [data-ais-workspace="true"] > [data-ais-role="code"],
                [data-ais-workspace="true"] > [data-ais-col-index="1"] {
                    display: none !important;
                }
            """
            StudioViewMode.SPLIT_CHAT_PREVIEW -> """
                [data-ais-workspace="true"] > [data-ais-role="chat"],
                [data-ais-workspace="true"] > [data-ais-col-index="0"] {
                    display: flex !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                    min-width: 300px !important;
                }
                [data-ais-workspace="true"] > [data-ais-role="code"],
                [data-ais-workspace="true"] > [data-ais-col-index="1"] {
                    display: none !important;
                }
                [data-ais-workspace="true"] > [data-ais-role="preview"],
                [data-ais-workspace="true"] > [data-ais-col-index="2"] {
                    display: flex !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                    min-width: 300px !important;
                }
            """
            StudioViewMode.SPLIT_CODE_PREVIEW -> """
                [data-ais-workspace="true"] > [data-ais-role="chat"],
                [data-ais-workspace="true"] > [data-ais-col-index="0"] {
                    display: none !important;
                }
                [data-ais-workspace="true"] > [data-ais-role="code"],
                [data-ais-workspace="true"] > [data-ais-col-index="1"] {
                    display: flex !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                    min-width: 300px !important;
                }
                [data-ais-workspace="true"] > [data-ais-role="preview"],
                [data-ais-workspace="true"] > [data-ais-col-index="2"] {
                    display: flex !important;
                    width: 50% !important;
                    max-width: 50% !important;
                    flex: 1 1 50% !important;
                    min-width: 300px !important;
                }
            """
        }

        val cleanCss = cssRules.replace("\n", " ").replace("\"", "\\\"").trim()

        return """
            (function() {
                let styleTag = document.getElementById('studio-tablet-overrides');
                if (!styleTag) {
                    styleTag = document.createElement('style');
                    styleTag.id = 'studio-tablet-overrides';
                    document.head.appendChild(styleTag);
                }
                styleTag.textContent = "$cleanCss";
                if (window.scanAndTagColumns) {
                    window.scanAndTagColumns();
                }
            })();
        """.trimIndent()
    }
}
