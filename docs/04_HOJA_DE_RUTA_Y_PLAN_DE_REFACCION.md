# 🚀 ODYSSEY — Hoja de Ruta Oficial (Plan Maestro de 5 Versiones)

El ciclo de vida de **ODYSSEY (Smart Destination Platform & Concierge IA para Cartagena de Indias)** se gobierna mediante **Versionado Semántico (`SemVer`)** en un único repositorio oficial (`josezambranol/Odyssey`), completando sus **5 macro-versiones evolutivas**:

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

## ✅ Versión `v4.0.0` / `v4.0.1` — Inteligencia Vectorial, Clima Marino, Web Push & Blindaje Legal *(Completada)*
- [x] **Búsqueda Híbrida SQL + Semántica Vectorial (`/buscar` + `/api/v1/lugares/semantico`):** Vectores densos L2-normalizados (`256d` + *Gemini `text-embedding-004`*) y similitud del coseno ($\cos\theta$).
- [x] **Inteligencia Climática Marina & UV (`MarineWeatherService` + `/api/v1/clima/marino`):** Semáforo náutico para Islas del Rosario, viento en nudos y caché TTL de 15 minutos.
- [x] **Notificaciones Web Push PWA (VAPID `P-256`):** Claves criptográficas EC `secp256r1` nativas, persistencia JPA y alertas automáticas de eventos culturales.
- [x] **Blindaje Legal Colombiano (`v4.0.1`):** Prueba de consentimiento Decreto 1377, bloqueo `<18` años, autoservicio **ARCO** (portabilidad JSON + eliminación de cuenta en `/menu`), cláusula **ESCNNA (`Ley 679 de 2001`)** y `MIT License`.
- [x] **Blueprint Cloud \$0.00 sin Suspensión (`render.yaml`):** Hibernación bajo demanda con `-XX:+UseSerialGC` optimizado para 512 MB RAM.

---

## ✅ Versión `v5.0.0` — Smart Destination, Visión Multimodal & Pases QR Criptográficos *(Completada)*
- [x] **Escáner Inteligente con *Gemini Multimodal Vision* (`/escaner` + `/api/v1/vision/analizar`):** Reconocimiento fotográfico patrimonial en memoria RAM y **Escudo Anti-Sobrecobros** frente a tarifas oficiales de referencia (Ley 1480 Art. 26).
- [x] **Pases Turísticos QR Firmados con `HMAC-SHA256` & Control RNT (`/api/v1/pases/emitir` + `/verificar`):** Verificación criptográfica sin estado en tiempo constante (`MessageDigest.isEqual`), sello QR vectorial SVG y validación de formato **RNT (`Ley 2068 de 2020`)**.
- [x] **Pipeline DevSecOps Completo (`ci.yml` + `JaCoCo`):** Reporte automático de cobertura de código y verificación de construcción multi-etapa Docker en GitHub Actions.
