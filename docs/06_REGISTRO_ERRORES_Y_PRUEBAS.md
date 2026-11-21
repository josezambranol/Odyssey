# 06. Registro de Errores, Pruebas y Fallas — ODYSSEY

Bitácora centralizada para documentar fallas detectadas, pruebas ejecutadas (automatizadas y manuales en navegador), causa raíz, solución aplicada y estado actual.

---

## Formato para Nuevos Registros

Copiar y completar este bloque al reportar una nueva falla o corrida de pruebas:

```markdown
### [ERR-XXX] Título breve del fallo o prueba
- **Fecha:** YYYY-MM-DD
- **Origen:** (QA Navegador / Maven Test / Docker / Producción)
- **Ruta / Componente:** (`/ruta` o `Clase.java`)
- **Severidad:** Alta | Media | Baja
- **Estado:** Abierto | En progreso | Resuelto | Bloqueado
- **Descripción:** Qué ocurrió y cómo reproducirlo.
- **Causa raíz:** Motivo técnico identificado.
- **Solución / Evidencia:** Cambio realizado y resultado tras verificar.
```

---

## Historial de Pruebas Ejecutadas

| Fecha | Tipo de Prueba | Alcance | Resultado | Notas / Bloqueos |
| :--- | :--- | :--- | :--- | :--- |
| 2026-11-19 | QA Humano en Navegador | `http://localhost:8080` (anónimo, usuario registrado y admin) | 12 hallazgos documentados en Fase 7 | Originó los bloques 7.1, 7.2 y 7.3 en `docs/05_PLAN_FASES_Y_REGLAS.md` |
| 2026-11-19 | Suite Maven (`mvn -B clean package`) | Perfil `test` (H2): `AuthWorkflowIntegrationTest`, REST controllers y servicios | **33/33 pasaron** (`0` fallos, `0` errores, `0` omitidos) | Ejecutado tras finalizar las ediciones de la Fase 7 |
| 2026-11-19 | Verificación Contenedores Docker | `docker ps` | **Pendiente / Daemon apagado** | Docker Desktop detenido (`open //./pipe/dockerDesktopLinuxEngine`); pendiente reconstruir stack (`docker compose up -d --build`) y re-testear en navegador |

---

## Registro de Fallas y Correcciones

### [ERR-001] `TypeError` en consola JS al cargar `/admin`
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Alta · **Estado:** Resuelto (pendiente re-verificación en contenedor)
- **Ruta / Componente:** `/admin` (`src/main/resources/templates/admin.html`)
- **Descripción:** En cada carga de `/admin` aparecía `Uncaught TypeError: Cannot read properties of null (reading 'classList')` en el listener de `scroll`.
- **Causa raíz:** El script intentaba leer `document.getElementById('navbar')`, elemento inexistente porque `/admin` utiliza el fragmento `fragments :: navbar-interno`.
- **Solución:** Añadida guarda `if (navbar)` antes de registrar el evento `scroll`.

### [ERR-002] Emojis corruptos (`?` / `?️`) en catálogo y respuestas del chatbot
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Alta · **Estado:** Resuelto en SQL (`sql/odyxsvg_db.sql`)
- **Ruta / Componente:** `sql/odyxsvg_db.sql` (`categorias`, `chatbot_respuestas`)
- **Descripción:** Los emojis de respuestas locales del chatbot y categorías aparecían como `?` / `?️`.
- **Causa raíz:** Cabecera `/*!50503 SET NAMES utf8 */;` y registros volcados previamente sin `utf8mb4`.
- **Solución:** Actualizado `SET NAMES utf8mb4` y restaurados los emojis UTF-8 válidos en `sql/odyxsvg_db.sql`. *(Nota: si existe un volumen MySQL previo, requiere recrear el volumen o reimportar el script SQL).*

### [ERR-003] Modal `"Editar Lugar"` sin campos `latitud` / `longitud`
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Alta · **Estado:** Resuelto (verificado en compilación y tests)
- **Ruta / Componente:** `/admin` (`admin.html` + `AdminController.editarLugar`)
- **Descripción:** No era posible visualizar ni actualizar coordenadas geográficas al editar un lugar existente.
- **Solución:** Añadidos atributos `th:data-latitud` / `th:data-longitud`, campos de formulario en `#modalEditarLugar` y parámetros `latitud` / `longitud` en `AdminController.editarLugar`.

### [ERR-004] Doble alerta de éxito en `/login?registered=true`
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Alta · **Estado:** Resuelto
- **Ruta / Componente:** `/login` (`src/main/resources/templates/login.html`)
- **Descripción:** Tras registrarse se mostraban dos banners verdes redundantes.
- **Solución:** Unificadas ambas condiciones en una única alerta limpia.

### [ERR-005] Desbordamientos, banners atrapados y recorte en `/admin`
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Media · **Estado:** Resuelto
- **Ruta / Componente:** `/admin` (`admin.html`)
- **Descripción:** Tablas de Eventos y Actividades sobresalían por la derecha; sub-banners `"Gestion de eventos"` y `"Gestion de actividades"` quedaban dentro de `.container` y sin tilde; `<input type="file">` de `#modalEditarActividad` se truncaba.
- **Solución:** Envueltas todas las tablas `.admin-table` en `.table-responsive`, cerrados/reabiertos los `.container` fuera de cada `<header class="page-header">`, corregidas las tildes y pasado el selector de archivo a `.odyx-field.full`.

### [ERR-006] Solapamientos de UI en `/mapa`, `/` y ajustes de controles (`<select>`, `<textarea>`, `/registro`, `Salir`)
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Media · **Estado:** Resuelto
- **Ruta / Componente:** `mapa.html`, `fragments.html`, `odyssey-travela.css`, `transporte.html`, `registro.html`, `index.html`, `chatbot.html`
- **Descripción:** Colisión entre `.hud-top-left` y `.hud-top-right` en `/mapa`; recorte de texto en tarjetas laterales; footer tapado por `#chatFab`/`.back-to-top`; doble flecha en `<select>` en `/transporte` y `/registro`; salto de línea en `"Estilo"` en `/`; placeholder recortado en `/chatbot`; caption de `/registro` debajo del pliegue a `1280x800`; bajo contraste en `"Salir"`.
- **Solución:** Ajustados anchos/espaciados de widgets en `/mapa`, clamp de 2 líneas, `.odyssey-footer-safe`, `-webkit-appearance`/`appearance: none !important`, columnas responsivas en itinerario, `min-height: 52px` en `/chatbot`, `.auth-left` sticky a `100vh` y botón `btn-outline-light` para `"Salir"`.

### [ERR-007] Pluralización, `<code>JSESSIONID</code>` y categoría `FESTIVALES` vacía
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Media · **Estado:** Resuelto
- **Ruta / Componente:** `detalle.html`, `buscar.html`, `mapa.html`, `privacidad.html`, `IndexController.java`, `OdysseyDataSeeder.java`, `sql/odyxsvg_db.sql`
- **Descripción:** Contadores mostraban plural para `1`; `/privacidad` mostraba backticks literales `` `JSESSIONID` ``; pestaña `FESTIVALES` aparecía sin lugares.
- **Solución:** Reglas singular/plural en vistas y JS, etiqueta `<code>JSESSIONID</code>`, sede oficial para `FESTIVALES` (`Centro de Convenciones Cartagena de Indias`) y filtro `categoriasConLugares` en `/`.

### [ERR-008] Cobertura incompleta `ES / EN` al retirar Google Translate
- **Fecha:** 2026-11-19 · **Origen:** QA Navegador · **Severidad:** Media · **Estado:** Resuelto
- **Ruta / Componente:** `fragments.html`, `index.html`, `menu.html`, `transporte.html`, `chatbot.html`, `lugares.html`, `messages*.properties`
- **Descripción:** Permanecían textos en español (topbar, hero, buscador, `/menu`, `/transporte`, `/chatbot`), título de `/lugares` sin traducir y título duplicado en `/chatbot`.
- **Solución:** Eliminados cookies residuales `googtrans`, añadidas claves en `messages.properties` / `messages_en.properties` e integradas en todas las plantillas y bloques JS (`th:inline="javascript"`).

---

## Auditoría Técnica y Pruebas de Rendimiento PC vs Móvil (2026-11-20)

### [ERR-009] Ruta rota (`404`) en botón `"Sincronizar en Tiempo Real"` de `/eventos`
- **Fecha:** 2026-11-20 · **Origen:** Auditoría estática + QA navegador · **Severidad:** Alta · **Estado:** Resuelto y verificado
- **Ruta / Componente:** `/eventos/sincronizar` (`eventos.html` + `TurismoController.java`)
- **Causa raíz:** `eventos.html` apuntaba a `/eventos/sincronizar`, pero ningún controlador exponía ese endpoint.
- **Solución:** Añadido `@GetMapping("/eventos/sincronizar")` en `TurismoController` llamando a `eventoSyncService.refrescarCarteleraEventos()` y redirigiendo a `/eventos?synced=true` (verificado con HTTP `200` en navegador y test de integración en `AuthWorkflowIntegrationTest`).

### [ERR-010] Fuga de lugares `PENDIENTE` / `RECHAZADO` y endpoints REST de reportes/itinerarios sin restricción
- **Fecha:** 2026-11-20 · **Origen:** Auditoría de seguridad · **Severidad:** Alta · **Estado:** Resuelto y cubierto por tests
- **Ruta / Componente:** `LugarController.java`, `LugarRestController.java`, `SecurityConfig.java`, `ItinerarioRestController.java`
- **Causa raíz:** `/lugares/{id}` y `/api/v1/lugares/{id}` no validaban `estado == APROBADO`; `/api/**` exponía `GET /api/v1/reportes/**` y `GET /api/v1/itinerarios/usuario/{usuarioId}` sin autenticación ni verificación de titularidad.
- **Solución:** Restringidos lugares no aprobados en vista (solo `ADMIN`) y API (`404`), bloqueado `/api/v1/reportes/**` a `ROLE_ADMIN`, exigida sesión y validación de titularidad/ADMIN en `/api/v1/itinerarios/usuario/{usuarioId}`.

### [ERR-011] Desfase diario en caché de eventos y `@Cacheable` mal ubicado en lugares
- **Fecha:** 2026-11-20 · **Origen:** Auditoría técnica · **Severidad:** Media · **Estado:** Resuelto
- **Ruta / Componente:** `EventoSyncService.java`, `LugarService.java`, `LugarRestController.java`
- **Solución:** Clave diaria `T(java.time.LocalDate).now().toString()` en `eventosClasificados` y traslado de `@Cacheable(value = "lugares", key = "'aprobados'")` desde el controlador REST a `LugarService.obtenerAprobados()`.

### [ERR-012] Validación faltante en reseñas (>500 chars), fechas pasadas en propuestas y silenciamiento de errores en `/admin`
- **Fecha:** 2026-11-20 · **Origen:** Auditoría técnica · **Severidad:** Media · **Estado:** Resuelto
- **Ruta / Componente:** `ResenaController.java`, `detalle.html`, `AdminController.java`, `admin.html`, `TurismoController.java`
- **Solución:** Validación `1..500` caracteres (`maxlength="500"`) y mensajes flash en reseñas; rechazo de fechas anteriores a hoy en `/proponer-evento`; `RedirectAttributes` (`mensajeAdmin`/`errorAdmin`) y validación de esquema URL en `/admin`.

### [ERR-013] Referencias residuales (Esri, MongoDB, Redis, 2dsphere, 26 sitios), código muerto y columnas faltantes en `sql/odyxsvg_db.sql`
- **Fecha:** 2026-11-20 · **Origen:** Auditoría estática · **Severidad:** Baja · **Estado:** Resuelto
- **Ruta / Componente:** `terminos.html`, `ReporteRestController.java`, `EventoRestController.java`, `GeminiAiService.java`, `AuthCodigoService.java`, `UsuarioController.java`, `fragments.html`, `mapa.html`, `index.html`, `sql/odyxsvg_db.sql`
- **Solución:** Eliminadas referencias obsoletas en Términos, Swagger y logs; retirados `UsuarioController.logout` muerto y fragmento no usado `footer-chatbot-only`; añadidas `latitud`, `longitud` y `visitas` al `CREATE TABLE lugares` y su seed en `sql/odyxsvg_db.sql`.

### [ERR-014] Cuellos de botella de rendimiento en PC (`1440×900`) y Móvil (`390×844`) — 13.29 MiB en `/` y `Cache-Control: no-store`
- **Fecha:** 2026-11-20 · **Origen:** Prueba de rendimiento en navegador (`PC 1440×900` y `Móvil 390×844`) · **Severidad:** Alta · **Estado:** Resuelto
- **Ruta / Componente:** `static/img/*`, `WebConfig.java`, `application.properties`, `odyssey-travela.css`, `index.html`, `lugares.html`, `eventos.html`, `actividades.html`, `admin.html`
- **Descripción:**
  - La portada `/` descargaba **13.29 MiB** en 17 recursos locales (`hero-castillo.jpg` 3.91 MiB oculto por CSS, `hotel-charleston.jpg` 2.92 MiB, `chatbot-icon.png` 2.54 MiB en un botón de `58×58 px` cargado en todas las páginas, `gastro-arepa.jpg` 1.02 MiB).
  - Todos los estáticos salían con `cache-control: no-cache, no-store, max-age=0`, obligando a re-descargar imágenes en cada navegación.
  - `0` imágenes tenían `loading="lazy"`, faltaba compresión HTTP GZIP, había `@import` bloqueante duplicado de Google Fonts en `odyssey-travela.css`, `bootstrap-icons.css` sin usar y `odyxs-travela.css` muerto (`33.4 KB`).
- **Solución:**
  - Optimizadas y redimensionadas 10 imágenes pesadas: `chatbot-icon.png` (`2.597,6 KB → 37,6 KB`), `hero-castillo.jpg` (`4.002 KB → 310,6 KB`), `act-kayak-manglares.jpg` (`3.139 KB → 412,4 KB`), `hotel-charleston.jpg` (`2.992 KB → 207,5 KB`), `gastro-arepa.jpg` (`1.046 KB → 109,3 KB`), reduciendo el directorio `static/img/` de **19.30 MB a 5.06 MB (-73.8%)**.
  - Habilitado `server.compression.enabled=true` (GZIP) en `application.properties` y `CacheControl.maxAge(30d).cachePublic()` en `WebConfig` para `/img/**`, `/css/**`, `/js/**` (y `7d` para `/uploads/**`).
  - Retirado `@import` duplicado en `odyssey-travela.css`, `bootstrap-icons.css`, `odyxs-travela.css` y la imagen oculta `hero-castillo.jpg` de `.hero-mosaic`; añadido `loading="lazy" decoding="async"` a imágenes bajo el pliegue.

### [ERR-015] Colapso de barra de búsqueda en `/mapa` y solapamiento de botones flotantes en Móvil (`390×844`)
- **Fecha:** 2026-11-20 · **Origen:** Prueba responsive en navegador (`Móvil 390×844`) · **Severidad:** Alta · **Estado:** Resuelto
- **Ruta / Componente:** `mapa.html`, `odyssey-travela.css`, `fragments.html`, `chatbot.html`
- **Descripción:**
  - En `/mapa` a `390×844`, `.hud-top-left` heredaba `max-width: calc(100% - 294px)` quedando colapsada a `47.6 px` de ancho, y `.hud-bottom-left` tapaba los botones de zoom `+`/`-` de Leaflet.
  - `#chatFab` (`58×58`) y `.back-to-top` tapaban controles primarios en el primer pantallazo móvil (botón `Buscar`, pestañas de `/eventos`, select de `/transporte`) y `#chatFab` aparecía incluso dentro de `/chatbot`.
- **Solución:** Restablecido `max-width: none` en `@media (max-width: 991px)` y ancho `calc(100% - 76px)` en `.hud-bottom-left` de `/mapa`; ocultado `#chatFab`/`#chatPopup` dentro de `/chatbot`; reducido `#chatFab` a `46×46` en móvil y activado `.back-to-top` solo tras hacer scroll (`window.scrollY > 300`).
