# Studio Tablet - Google AI Studio para Tablets

Entorno móvil optimizado para tablets que ejecuta la plataforma oficial de **Google AI Studio** en modo escritorio, incorporando un sistema de inyección reactivo y controles táctiles para alternar instantáneamente entre vistas de pantalla completa: **Chat (100%)**, **Código (100%)**, **Preview (100%)** y **Modos Split (50/50)**.

---

## Ajustes Quirúrgicos Implementados

1. **Protección Total de la Barra Superior & Botón de Engranaje:**
   - La barra de cabecera (`header`, `[role="banner"]`, `mat-toolbar`) ahora está explícitamente protegida: **nunca se destruye ni se le aplica `display: none`**.
   - Mantiene `z-index: 1000`, `position: relative` y `pointer-events: auto`.
   - Se habilitó un botón en la barra nativa para alternar entre **Modo Normal** y **Modo Compacto (40px)**, permitiendo ganar espacio vertical sin desconectar los escuchadores de clic ni bloquear el menú de configuración/engranaje.
   - Todos los contenedores de menús emergentes y popovers de Angular (`.cdk-overlay-container`, `.mat-mdc-menu-panel`, `[role="menu"]`, `[role="dialog"]`) tienen `z-index: 999999 !important` para que el menú de ajustes siempre se dibuje por encima de todo.

2. **Preservación del Chat & Historial de Conversación (Anti-Virtual Scroll Collapse):**
   - Se eliminó por completo el uso de `display: none !important` en las columnas no activas.
   - Se sustituyó por un **colapsado suave no destructivo**:
     `width: 0px !important; min-width: 0px; max-width: 0px; flex: 0 0 0px; overflow: hidden; opacity: 0; pointer-events: none; visibility: hidden;`
   - Los contenedores de mensajes del chat (`cdk-virtual-scroll-viewport`, `.chat-turn-list`, `.messages-container`) y la barra de sugerencias tienen reglas explícitas de visibilidad y altura mínima para que el motor de virtual scrolling no detecte 0px ni desmonte el historial de mensajes.

3. **Herramienta de Diagnóstico "Inspector DOM" en Vivo:**
   - Se añadió un botón en la barra superior (icono `{}`) que ejecuta un escaneo en vivo del HTML de Google AI Studio y muestra un diálogo con las etiquetas, clases y elementos detectados (con botón de **Copiar Diagnóstico** al portapapeles).
