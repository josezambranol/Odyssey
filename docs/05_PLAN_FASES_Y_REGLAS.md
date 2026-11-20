# ODYSSEY — Plan de Trabajo por Fases y Reglas de Ejecución

> **Propósito de este documento:** servir como lista de reglas obligatorias, inventario de archivos y criterios de verificación para refactorizar ODYSSEY hacia un stack limpio (**Java 21 + Spring Boot 3.2.5 + MySQL 8**), sin imágenes rotas/repetidas y en cumplimiento legal y de seguridad.
> **Regla de oro:** ninguna fase se marca como terminada hasta pasar sus criterios de aceptación (`mvn -B test` y verificación en vivo sobre Docker).

---

## 0. Reglas Globales Obligatorias (Aplican a TODAS las fases)

1. **Stack objetivo único:** Java 21 + Spring Boot 3.2.5 + Spring Data JPA + MySQL 8 + Thymeleaf.
2. **Cero sobre-ingeniería:** queda prohibido agregar o reintroducir MongoDB, Redis, Jsoup/scraping simulado, `pollinations.ai` o contenedores de administración auxiliares (`mongo-express`, `redis-commander`).
3. **Cero rutas rotas o duplicados:** toda imagen referenciada en BD, Java o plantillas HTML debe existir en `src/main/resources/static/img/` (o subirse mediante `FileStorageService` con archivo real). Prohibido usar URLs externas de imágenes sin licencia verificada.
4. **Cumplimiento legal y de marca:**
   - Prohibido usar dominios gubernamentales (`cartagena.gov.co`) o suplantar entidades oficiales.
   - Todo formulario que capture datos personales (registro, OTP, propuestas, suscripciones) debe exigir aceptación explícita de la Política de Privacidad (Ley 1581 de 2012) y Términos de Uso.
   - Prohibido guardar contraseñas en texto plano en código, propiedades o `sql/odyxsvg_db.sql`.
5. **Control de cambios por fase:** trabajar una sola fase a la vez; antes de pasar a la siguiente, actualizar el checklist `[x]` de este archivo y validar compilación, pruebas (`mvn -B test`) y endpoints en `http://localhost:8080`.
6. **Preservación funcional:** no romper rutas públicas vigentes (`/`, `/guia`, `/lugares`, `/actividades`, `/eventos`, `/mapa`, `/transporte`, `/chatbot`, `/buscar`, `/login`, `/registro`, `/admin/**`, `/api/v1/**`).

---

## Fase 1 — Saneamiento Legal, Privacidad, Seguridad Base y Dump SQL

### Objetivo
Corregir riesgos legales, de suplantación y de seguridad antes de refactorizar bases de datos, y dejar `sql/odyxsvg_db.sql` 100% compatible con MySQL 8.

### Tareas y Reglas
- [x] **1.1 Documentos legales obligatorios:**
  - Crear vistas públicas `/terminos` y `/privacidad` (Thymeleaf) y enlazarlas en el footer común (`templates/fragments.html`).
  - Incluir en `/privacidad` la información exigida por la **Ley 1581 de 2012 (Colombia)**: finalidad del tratamiento (correo, nombre, país, fecha de nacimiento, logs de consulta), uso de cookies técnicas (`JSESSIONID`), transferencia a terceros (Google Gemini / SMTP cuando estén activos) y canal de contacto para supresión/rectificación.
  - Agregar checkbox obligatorio de aceptación de Términos y Política de Privacidad en `registro.html` (y aviso en flujo OTP / formularios de propuestas).
- [x] **1.2 Eliminar suplantación de dominio oficial:**
  - En `src/main/java/com/odyxs/vg/config/OpenApiConfig.java`, reemplazar `https://odyssey.cartagena.gov.co` y `https://api.odyssey.cartagena.gov.co` por URLs propias del proyecto o `http://localhost:8080` + variable configurable.
  - En `src/main/java/com/odyxs/vg/service/EventoSyncService.java`, eliminar la referencia falsa a `https://cartagena.gov.co/cultura-y-turismo`.
- [x] **1.3 Endurecimiento de `SecurityConfig.java`:**
  - Mantener CSRF habilitado en formularios web y revisar excepciones en `/api/**` y `/auth/codigo/**`.
  - Proteger endpoints administrativos o de estrés (`POST /api/v1/reportes/benchmark/seed-15k`, `POST /api/v1/eventos/sync` si aplica) bajo `hasRole("ADMIN")`.
  - Verificar que `/proponer-*` y formularios públicos tengan validación `@Valid`, límites de tamaño y sanitización.
- [x] **1.4 Corrección y limpieza de `sql/odyxsvg_db.sql`:**
  - Cambiar `DEFAULT curdate()` en la tabla `resenas` (línea 176) a `DEFAULT (curdate())` para compatibilidad con MySQL 8.
  - Reemplazar contraseñas en texto plano del dump (`admin2026`, `usuario`, `12345`, `123456`) por hashes **BCrypt** válidos (o eliminar usuarios de prueba inseguros y dejar que `OdysseyDataSeeder` inicialice el admin desde variables de entorno).
  - Asegurar que `resenas` y `usuarios` se importen completos al inicializar `odyssey_mysql` desde cero.

### Criterio de Verificación
- Recrear volumen MySQL limpio y verificar `0` errores SQL en logs de `odyssey_mysql`.
- `GET /terminos` y `GET /privacidad` responden `200`.
- `POST /api/v1/reportes/benchmark/seed-15k` sin sesión `ADMIN` devuelve `401`/`403` (o redirección a login).

---

## Fase 2 — Reparación Integral de Imágenes y Eliminación de Duplicados

### Objetivo
Eliminar el `100%` de imágenes rotas (`404`), quitar duplicados binarios y evitar que las tarjetas repitan la misma foto por fallos de carga.

### Inventario Detectado
- Rutas `/uploads/...` inexistentes en `sql/odyxsvg_db.sql` (11 lugares, 7 actividades, 6 eventos).
- Ruta inexistente `/img/bocagrande.jpg` en `templates/buscar.html` (líneas 44-45).
- Ruta inexistente `/uploads/lugares/castillo.jpg` en `LugarGeoService.java` (línea 76).
- Duplicados binarios en `src/main/resources/static/img/`:
  - `apple-touch-icon.png` ≡ `favicon-180x180.png`
  - `favicon-32.png` ≡ `favicon-32x32.png`
- Referencias repetidas en `LugarGeoService.java` y `ExternalEventCrawlerService.java`.

### Tareas y Reglas
- [x] **2.1 Normalizar `static/img/`:**
  - Eliminar archivos binarios redundantes o unificar referencias (`favicon-180x180.png` / `apple-touch-icon.png` y `favicon-32.png` / `favicon-32x32.png`) verificando que ninguna plantilla quede con ruta huérfana.
  - Documentar en `src/main/resources/static/img/README.txt` la fuente y licencia (CC0 / Unsplash / Wikimedia / Pixabay) de cada imagen conservada; eliminar o reemplazar cualquier imagen cuya licencia o marca comercial sea dudosa.
- [x] **2.2 Corregir rutas en BD y código Java:**
  - Actualizar `sql/odyxsvg_db.sql` para que cada lugar, actividad y evento semilla apunte a una imagen real y diferenciada dentro de `/img/...` (sin depender de `/uploads/` efímeros).
  - Corregir `/img/bocagrande.jpg` en `templates/buscar.html` hacia una imagen existente (ej. `/img/act-playa-bocagrande.jpg`).
  - Corregir `/uploads/lugares/castillo.jpg` en `LugarGeoService.java` hacia `/img/hero-castillo.jpg` y asignar fotos acordes a cada punto georreferenciado.
  - Revisar `templates/index.html` para evitar duplicación innecesaria de tarjetas del mismo catálogo en la página de inicio si confunde al usuario.

### Criterio de Verificación
- Script de auditoría sobre `/`, `/lugares`, `/actividades`, `/eventos`, `/guia`, `/mapa` y `/buscar`: **0** respuestas `404` en todas las etiquetas `<img src="...">`.
- Cada tarjeta del catálogo inicial muestra su imagen correspondiente y no el fallback genérico `hero-murallas.jpg`.

---

## Fase 3 — Simplificación de Stack: Eliminación de MongoDB y Jsoup (Migración a MySQL 8)

### Objetivo
Consolidar toda la persistencia en **MySQL 8** con JPA/Hibernate y eliminar el motor MongoDB, `mongo-express` y el falso crawler con `Jsoup`.

### Archivos Afectados
- `pom.xml`: retirar `spring-boot-starter-data-mongodb` y `org.jsoup:jsoup`.
- `docker-compose.yml`: retirar servicios `mongodb` y `mongo-express`, volumen `mongo_data` y `MONGO_URI`.
- `src/main/java/com/odyxs/vg/config/MongoConfig.java` y `JpaConfig.java`: limpiar configuración MongoDB.
- `domain/mongo/*` y `repository/mongo/*`:
  - `Itinerario` → entidad JPA `@Entity` en MySQL (`itinerarios` + tabla hija o JSON/texto estructurado para días).
  - `CodigoVerificacion` → entidad JPA en MySQL (`codigos_verificacion`) con expiración por `Instant`.
  - `SuscripcionLugar` → entidad JPA en MySQL (`suscripciones_lugares`).
  - `ChatbotLog` → entidad JPA en MySQL (`chatbot_logs`, anonimizando o evitando almacenar IP completa si no es necesaria).
  - `Reporte` → entidad JPA en MySQL (`reportes`) conservando exportación CSV por streaming/paginación (`ReporteService` sin `MongoTemplate`).
  - `LugarGeoDocument` / `LugarGeoService` → agregar `latitud` y `longitud` a `Lugar` (o tabla `lugares_geo` en MySQL) y calcular cercanía por fórmula **Haversine** en Java/SQL (sin `2dsphere`).
  - `EventoScrapingLog` y `ExternalEventCrawlerService` → **eliminar** (y en `EventoSyncService`, mantener solo gestión de agenda desde MySQL sin simular scraping externo).
- `application.properties` y `application-test.properties`: limpiar propiedades `spring.data.mongodb.*`.

### Tareas y Reglas
- [x] **3.1** Migrar entidades y repositorios necesarios a JPA/MySQL manteniendo los contratos de `ApiResponse` y endpoints `/api/v1/**` (o ajustando pruebas unitarias/integración acordemente).
- [x] **3.2** Eliminar `ExternalEventCrawlerService`, `EventoScrapingLog` y dependencias `jsoup` y `spring-boot-starter-data-mongodb`.
- [x] **3.3** Actualizar `OdysseyDataSeeder` y `ReporteService` para operar 100% sobre MySQL.
- [x] **3.4** Actualizar `docker-compose.yml` y `application.properties`.

### Criterio de Verificación
- `mvn -B test` verde sin MongoAutoConfiguration ni clases `mongo`.
- `docker compose up -d --build` levanta sin `odyssey_mongodb` ni `odyssey_mongo_express`.
- `/mapa` (radar cercano Haversine), `/chatbot`, `/auth/codigo/**`, `/suscripciones/**` y `/api/v1/itinerarios` funcionan contra MySQL.

---

## Fase 4 — Eliminación de Redis y Reemplazo por Caché / Métricas Nativas

### Objetivo
Quitar el servidor Redis y `redis-commander`, reemplazando su función con caché en memoria nativa de Spring Boot y contadores simples en MySQL.

### Archivos Afectados
- `pom.xml`: retirar `spring-boot-starter-data-redis` y `org.apache.commons:commons-pool2`.
- `docker-compose.yml`: retirar `redis`, `redis-commander`, volumen `redis_data`, `REDIS_HOST` y `REDIS_PORT`.
- `application.properties` y `config/RedisConfig.java`: eliminar `RedisConfig` y cambiar `spring.cache.type=simple` (o `caffeine` si se agrega `com.github.ben-manes.caffeine:caffeine` ligero en memoria JVM).
- `service/RedisMetricsService.java` y `MetricasRestController.java`: reemplazar por servicio de métricas en Java + columna `visitas` en `Lugar` (MySQL) y rate limiter en memoria (`ConcurrentHashMap` con ventana por minuto).

### Tareas y Reglas
- [x] **4.1** Eliminar `spring-boot-starter-data-redis` y `commons-pool2` de `pom.xml`.
- [x] **4.2** Reemplazar `RedisConfig` por `@EnableCaching` estándar en memoria (`ConcurrentMapCacheManager` o Caffeine).
- [x] **4.3** Persistir visitas de lugares en MySQL y mantener trending calculado desde MySQL/caché.
- [x] **4.4** Retirar `redis` y `redis-commander` de `docker-compose.yml`.

### Criterio de Verificación
- `mvn -B test` verde sin clases de Redis.
- El stack Docker corre únicamente con **2 contenedores**: `odyssey_mysql` y `odyssey_backend`.
- Endpoints de caché y métricas (`/api/v1/metricas/trending`, `/api/v1/metricas/visitas/{id}`) operan sin errores.

---

## Fase 5 — Limpieza de IA Externa, Refinamiento Web/Frameworks y Verificación E2E

### Objetivo
Cerrar deuda técnica de integraciones externas, alinear librerías frontend/backend y validar la aplicación completa extremo a extremo.

### Tareas y Reglas
- [x] **5.1 Chatbot IA limpio (`GeminiAiService.java`):**
  - Eliminar la llamada no autenticada a `https://text.pollinations.ai/openai`.
  - Mantener únicamente **Google Gemini API** (cuando exista `GEMINI_API_KEY`) + **motor RAG local determinístico** sobre MySQL.
- [x] **5.2 Auditoría de frameworks frontend y dependencias:**
  - Verificar en plantillas que todas usen la misma versión de Bootstrap / iconos (en el diagnóstico previo se vieron referencias mixtas en CSS/CDN).
  - Decidir con el usuario si `springdoc-openapi` (Swagger) y `spring-boot-starter-mail` permanecen activos o se activan por perfil.
- [x] **5.3 Verificación End-to-End y documentación:**
  - Actualizar `README.md` y `docs/02_ARQUITECTURA_Y_MODELO_DE_DATOS.md` para reflejar la arquitectura simplificada real (Java 21 + Spring Boot + MySQL 8).
  - Ejecutar `mvn -B clean test`, `mvn -B package`, reconstruir `docker compose up -d --build` y probar todas las rutas web y REST.

### Criterio de Verificación Final
- `mvn -B clean test` con `0` fallos.
- Contenedores activos: solo `odyssey_mysql` (`healthy`) y `odyssey_backend` (`healthy`).
- `0` errores 404 de imágenes, `0` contraseñas en claro en SQL, páginas `/terminos` y `/privacidad` operativas, y sin referencias a `cartagena.gov.co`, MongoDB, Redis, Jsoup ni Pollinations.

---

## Fase 6 — Hardening de Producción, Lógica de Negocio y Derechos de Autor

> **Regla:** dividido en 3 bloques independientes y verificables. Al finalizar, el usuario indicará cuándo abrir el navegador para pruebas humanas interactivas en `http://localhost:8080`.

### Bloque A — Seguridad y Bugs Críticos
- [x] **A.1 Persistencia unificada de uploads + magic bytes:**
  - Configurar `ODYSSEY_UPLOAD_DIR` compartido entre `FileStorageService` y `WebConfig` (`/app/uploads` en Docker via `docker-compose.yml`, `${user.home}/odyssey-uploads` en local).
  - Validar *magic bytes* reales (JPEG, PNG, WEBP, GIF) además de `Content-Type` y extensión.
- [x] **A.2 Corregir Open Redirect en `/idioma`:**
  - Validar que la redirección de `IdiomaController` solo permita rutas relativas locales del mismo sitio (nunca URLs externas en `Referer`).
- [x] **A.3 Proteger endpoints sensibles de configuración y categorías:**
  - Restringir `POST /chatbot/config-key` a `ROLE_ADMIN` (o ocultarlo en `chatbot.html` si no es admin) y proteger `CategoriaController` (`@PreAuthorize("hasRole('ADMIN')")` + redirección segura).
- [x] **A.4 Sincronización de caché (`@CacheEvict`):**
  - Invalidar caché `"lugares"` en `LugarService` (`aprobar`, `rechazar`, `eliminar`, `guardar`, `actualizarImagen`) y en `AdminController.editarLugar`, e invalidar `"eventosClasificados"` en `AdminController` al agregar, aprobar, rechazar, editar o eliminar eventos.

### Bloque B — Lógica de Negocio Real
- [x] **B.1 Enlaces reales de hoteles en `index.html`:**
  - Incorporar *Sofitel Legend Santa Clara* y *Bastión Luxury Hotel* al catálogo base en `sql/odyxsvg_db.sql` (con coordenadas) y enlazar las tarjetas de hoteles en `index.html` a IDs reales existentes.
- [x] **B.2 Buscador completo (`/buscar`):**
  - Ampliar `LugarRepository` + `LugarService.buscar` para buscar en `nombre`, `descripcion` y `ubicacion` (case-insensitive) sobre lugares `APROBADO`.
- [x] **B.3 Coordenadas en formularios de lugares:**
  - Agregar campos opcionales `latitud` y `longitud` en `proponer-lugar.html` y `admin.html` (`LugarController`, `AdminController`, `LugarService`) para que los nuevos lugares aprobados aparezcan en el mapa `/mapa`.
- [x] **B.4 Borrado seguro con integridad referencial:**
  - En `LugarService.eliminar`, limpiar reseñas (`ResenaRepository.deleteByLugarId`) y suscripciones (`SuscripcionLugarRepository.deleteByLugarId`) asociadas antes de borrar el lugar, evitando errores de llave foránea `500`.

### Bloque C — Derechos de Autor, Terceros y Privacidad
- [x] **C.1 Retirar widget de Google Translate y su cookie de rastreo:**
  - Eliminar el script externo `translate.google.com` y la cookie `googtrans` de `fragments.html` e `IdiomaController`, manteniendo el `i18n` nativo de Spring Boot.
- [x] **C.2 Capas de mapas abiertas sin riesgo comercial Esri:**
  - Retirar `server.arcgisonline.com` (Esri ArcGIS) de `mapa.html` y usar capas abiertas con atribución (OpenStreetMap + modo oscuro por filtro CSS sobre OSM).
- [x] **C.3 Minimización y retención de datos (Ley 1581):**
  - Anonimizar (hash SHA-256) la IP antes de guardarla en `chatbot_logs` (`GeminiAiService`) y programar limpieza automática de códigos OTP expirados.
- [x] **C.4 Reducir riesgo de copyright en testimonios/avatares y canal de retiro:**
  - Sustituir retratos ficticios `resena-usuario-*.jpg` en `index.html` por avatares con iniciales CSS/SVG, aclarar su carácter ilustrativo y añadir enlace de *Reportar contenido / derechos* en `detalle.html`.

---

## Fase 7 — Correcciones de QA Humano en Navegador (Backlog guardado)

> **Origen:** prueba interactiva completa en navegador (`http://localhost:8080`) simulando usuario anónimo, viajero registrado y administrador.

### Bloque 7.1 — Bugs Funcionales y Consola JS (Prioridad Alta)
- [x] **7.1.1 Error JS en cada carga de `/admin`:**
  - `Uncaught TypeError: Cannot read properties of null (reading 'classList')` en `/admin` (aprox. línea 1459 del HTML renderizado). Identificar el elemento nulo y agregar guarda segura.
- [x] **7.1.2 Encoding de emojis roto en respuestas del chatbot (`?` / `?️`):**
  - En `sql/odyxsvg_db.sql` (`chatbot_respuestas`) y/o respuestas de `GeminiAiService`, los emojis quedaron corruptos como `?` / `?️` por charset/dump previo (`utf8` vs `utf8mb4`). Restaurar emojis válidos en UTF-8 (`utf8mb4`) o usar texto limpio.
- [x] **7.1.3 Modal `"Editar Lugar"` en `/admin` sin `latitud` / `longitud`:**
  - El formulario de agregar lugar ya tiene `latitud`/`longitud`, pero el modal `#modalEditarLugar` (`admin.html` + `AdminController.editarLugar`) no permite editar ni ver las coordenadas de un lugar existente.
- [x] **7.1.4 Banners duplicados en `/login?registered=true`:**
  - Aparecen dos alertas verdes redundantes tras registrarse (`UsuarioController` + `login.html` `param.registered`). Dejar un único mensaje limpio.

### Bloque 7.2 — Diseño, Layout y Desbordamientos (Prioridad Media)
- [x] **7.2.1 Desbordamiento de tablas y banners truncados en `/admin`:**
  - Las tablas de **Gestión de Eventos** y **Gestión de Actividades** se salen por el borde derecho de su tarjeta contenedora (botón `Eliminar` sobresale); envolver en `.table-responsive` y corregir `overflow-x`.
  - Los sub-banners `"Gestion de eventos"` y `"Gestion de actividades"` quedan atrapados dentro del contenedor (no full-bleed) y les falta la tilde en *Gestión*.
  - En el modal `"Editar Actividad"`, el `<input type="file">` recorta su texto (`"Ning...nado"`).
- [x] **7.2.2 Solapamiento en `/mapa` y footer de `/`:**
  - En `/mapa`, la barra de filtros (`"13 sitios"` / `"Museos"`) se solapa detrás del widget flotante `"ESTILO DE MAPA"`.
  - En `/mapa`, las descripciones de las tarjetas derechas se cortan horizontalmente en el borde.
  - En `/`, los botones flotantes del chatbot / volver arriba tapan parte del texto derecho del footer.
- [x] **7.2.3 Doble flecha en `<select>` y ajustes menores de UI:**
  - En `/transporte` y `/registro`, los `<select>` muestran doble flecha desplegable (nativa + CSS).
  - En `/` (generador de itinerarios), el texto del `<select>` `"Estilo"` se parte en 2 líneas.
  - En `/chatbot`, el placeholder de 2 líneas del `<textarea>` se corta verticalmente en la parte inferior.
  - En `/registro` (`1280x800`), el caption izquierdo queda por debajo del pliegue inicial.
  - En el navbar oscuro, el botón `"Salir"` rojo tiene bajo contraste visual.
- [x] **7.2.4 Formato y pluralización:**
  - Corregir singular/plural en `/lugares/{id}` (`"1 visita reciente"`, `"1 reseña"`, `"1 suscrito"`) y `/buscar` (`"1 lugar encontrado"`).
  - En `/privacidad`, reemplazar backticks literales `` `JSESSIONID` `` por etiqueta HTML `<code>JSESSIONID</code>`.
  - Revisar pestaña `FESTIVALES` vacía en `/` y `/lugares` (agregar lugar/festival representativo o ocultar pestañas sin elementos).

### Bloque 7.3 — Cobertura i18n `ES` / `EN` (Prioridad Media)
- [x] **7.3.1 Completar traducciones `EN` faltantes al quitar Google Translate:**
  - Topbar (`"Patrimonio UNESCO 1984 · Parcial nublado"`), botón `"Salir"`, H1 del hero, `"Radar Cerca de Mí"`, `"Explorar por"`, opción `"Todas las categorías"`, `"Buscar"`, `"Tendencias hoy"`.
  - Páginas completas `/menu`, `/transporte` y `/chatbot` que hoy permanecen en español al cambiar a `EN`.
  - Corregir `<title>` de `/lugares` (`"Lugares — ODYSSEY"` en `EN`) y título duplicado en `/chatbot` (`"ODYSSEY — Cartagena Guide — ODYSSEY"`).


