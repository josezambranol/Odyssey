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
