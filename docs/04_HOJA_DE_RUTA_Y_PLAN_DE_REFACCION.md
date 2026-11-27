# 🚀 ODYSSEY — Hoja de Ruta Oficial (Plan Maestro de 5 Versiones)

El ciclo de vida de **ODYSSEY (Guía Turística Inteligente & Concierge IA para Cartagena de Indias)** se gobierna mediante **Versionado Semántico (`SemVer`)** en un único repositorio oficial (`josezambranol/Odyssey`), organizado en **5 macro-versiones evolutivas**:

---

## ✅ Versión `v1.0.0` — Monolito Académico Inicial *(Completada · 4.º–5.º Semestre)*
- [x] Arquitectura MVC base con Spring Boot, Thymeleaf y MySQL (`odyxsvg_db`).
- [x] Catálogo inicial de sitios turísticos, actividades, eventos y reseñas de 1 a 5 estrellas.
- [x] Chatbot local básico por coincidencias de palabras clave.

---

## ✅ Versión `v2.0.0` — Persistencia Unificada, Seguridad & Sistema Editorial Stitch *(Completada · 6.º–7.º Semestre)*
- [x] Evolución de marca a **Odyssey** con sistema de diseño editorial unificado en todas las páginas internas.
- [x] Persistencia relacional unificada y optimizada en **MySQL 8** (y H2 en perfil de pruebas) con exportación CSV en streaming (+15,000 reportes) y métricas en vivo.
- [x] Catálogo geoespacial de 26 sitios emblemáticos con cálculo exacto **Haversine**, privacidad de IPs (`SHA-256`) y páginas legales (`/terminos`, `/privacidad`).
- [x] Autenticación híbrida con contraseña BCrypt y código OTP por correo electrónico.

---

## ✅ Versión `v3.0.0` — Cloud-Native, Virtual Threads, SSE & PWA Offline-First *(Completada)*
- [x] **Java 21 Project Loom (`spring.threads.virtual.enabled=true`):** Concurrencia masiva no bloqueante para peticiones de IA, correo y streaming.
- [x] **Server-Sent Events (`GET/POST /api/v1/ai/stream`):** Streaming progresivo de tokens en tiempo real con medición de *Time-To-First-Token (TTFT)*.
- [x] **Progressive Web App (PWA) Offline-First:** Instalable en móvil/escritorio (`manifest.webmanifest`, `sw.js` y guía SOS `offline.html` con tarifas DATT y emergencias marítimas sin señal).
- [x] **Voz Neuronal Bilingüe (`Web Speech API`):** Dictado por micrófono (STT) y lectura en voz alta (TTS) en Español e Inglés.
- [x] **Observabilidad SRE:** `Spring Boot Actuator` + `Micrometer Prometheus` (`/actuator/health/liveness`, `/actuator/prometheus`) y cabeceras HTTP de seguridad modernas.

---

## ✅ Versión `v4.0.0` — Inteligencia Vectorial Semántica, Clima Marino & Web Push *(Completada)*
- [x] **Búsqueda Híbrida SQL + Semántica Vectorial (`/buscar` + `/api/v1/lugares/semantico`):** Vectores densos L2-normalizados de 256 dimensiones + integración con *Google Gemini Embeddings (`text-embedding-004`)* y **Similitud del Coseno** ($\cos\theta$) integrada directamente en la interfaz de búsqueda.
- [x] **Inteligencia Meteorológica y Marina Costera (`MarineWeatherService` + `/api/v1/clima/marino`):** Semáforo náutico (`VERDE`/`AMARILLO`/`ROJO`) para zarpe a Islas del Rosario, velocidad de viento en nudos, alerta de radiación UV y caché TTL en memoria de 15 minutos con respaldo resiliente.
- [x] **Rutas Peatonales y Náuticas en Vivo (`/mapa`):** Trazado interactivo sobre Leaflet con cálculo OSRM peatonal y rutas marítimas desde el Muelle de la Bodeguita.
- [x] **Notificaciones Web Push PWA (Estándar W3C + VAPID `P-256`):** Generación criptográfica EC `secp256r1` (`WebPushNotificationService`), persistencia JPA (`suscripciones_push`), listeners nativos en `sw.js` y emisión automática al publicar/aprobar eventos culturales.
- [x] **Infraestructura Cloud Pública \$0.00 sin Suspensión (`render.yaml` + `Dockerfile`):** Blueprint Docker optimizado en memoria (`-XX:+UseSerialGC`, `MaxRAMPercentage=70.0`) y libre de auto-pings para preservar las 750 h/mes gratuitas junto a MySQL Serverless (TiDB Cloud / Aiven).

---

## 🌟 Versión `v5.0.0` — Smart Destination & Visión Multimodal *(Proyección Futura)*
- [ ] **Cámara Inteligente con *Gemini Multimodal Vision* ("Apunta y Descubre"):** Reconocimiento fotográfico de baluartes, monumentos, gastronomía y verificación instantánea de cobros frente a tarifas oficiales DATT.
- [ ] **Ecosistema para Emprendedores Verificados (`ROLE_EMPRENDEDOR`) & Pases QR:** Gestión autónoma de cupos, horarios y reservas verificables por código QR anti-sobrecobros.
- [ ] **Autenticación Biométrica sin Fricción:** Integración de *Google OAuth2* y *Passkeys (WebAuthn)*.
- [ ] **Observatorio Abierto de Turismo Inteligente:** Mapas de calor anonimizados y analítica estacional para planeación turística de Cartagena de Indias.
