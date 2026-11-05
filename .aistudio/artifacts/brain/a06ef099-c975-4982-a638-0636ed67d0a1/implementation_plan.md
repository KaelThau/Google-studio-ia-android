# Studio Tablet - Google AI Studio para Tablets

Entorno móvil optimizado para tablets que ejecuta la plataforma oficial de **Google AI Studio** en modo escritorio, incorporando un sistema de inyección reactivo y controles táctiles para alternar instantáneamente entre vistas de pantalla completa: **Chat (100%)**, **Código (100%)**, **Preview (100%)** y **Modos Split (50/50)**.

---

## Decisiones Implementadas

- **Plataforma Real:** Carga directa de `aistudio.google.com` utilizando un User-Agent de **Desktop Chrome moderno** (`Chrome/131.0.0.0`), permitiendo el acceso completo al entorno de escritorio y evitando el bloqueo de navegador móvil o `403 disallowed_useragent`.
- **Soporte de Autenticación de Google:** Configuración de `CookieManager` con soporte de cookies de terceros y `WebChromeClient.onCreateWindow` para gestionar ventanas emergentes de OAuth y Google Sign-In.
- **Inyector de CSS/DOM Inteligente:**
  - Script que identifica dinámicamente los contenedores de Chat, Código y Preview.
  - Modos de pantalla completa:
    1. **Todo (Original):** Visualización estándar del escritorio con todos los paneles.
    2. **Chat Completo (100%):** Oculta el editor y el emulador para redactar prompts largos y revisar respuestas con ancho completo.
    3. **Código Completo (100%):** Maximiza el explorador de archivos y el editor Monaco al 100% de la pantalla para revisar y editar código sin recortes.
    4. **Preview Completo (100%):** Centra y maximiza el emulador de streaming o lienzo de previsualización al 100%.
    5. **Split Chat + Preview (50/50):** Panel dual para iterar prompts y ver cambios en tiempo real.
    6. **Split Código + Preview (50/50):** Panel dual para inspeccionar el código Kotlin y la UI interactiva lado a lado.
- **Controles Táctiles y Ergonomía:**
  - **Barra Superior Adaptativa:** Chips de selección de modo, controles de Zoom (-/+) con reseteo a 100%, botón de recarga, selector de URLs rápidas (Build, Prompts, Documentación) y diálogo de ayuda.
  - **Píldora Flotante (Floating Mode Pill):** Botonera flotante en la parte inferior para alternar de vista con un solo toque del pulgar sin tener que estirar la mano hacia la parte superior de la tablet.
  - **Indicador de Progreso:** Barra lineal para seguimiento de carga de página.
  - **Manejo de Errores:** Tarjeta con estado de desconexión y botón de reintento.
