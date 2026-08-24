# 🛡️ ODYSSEY — Diagnóstico de Seguridad y Deuda Técnica

## 1. Matriz de Riesgos y Vulnerabilidades Detectadas

| Vulnerabilidad | Severidad | Impacto | Estado de Mitigación |
| :--- | :---: | :--- | :--- |
| **Spring Security Bypassed (`permitAll`, `csrf.disable`)** | 🔴 Crítica | Acceso no autorizado por omisión en sesión manual y ataques CSRF en peticiones POST. | Implementando `SecurityFilterChain` real y tokens CSRF. |
| **Hardcoded API Key (Google Gemini)** | 🔴 Crítica | Exposición de cuota y claves en repositorio público/privado. | Extraído a variable de entorno `GEMINI_API_KEY`. |
| **Hardcoded Admin Email & Default Password** | 🔴 Crítica | Cuenta administrativa comprometida por defecto (`admin@odyssey.com` / `admin2026`). | Parametrizado mediante variables y control de acceso estándar. |
| **Falta de Validación de Entrada (Bean Validation)** | 🟡 Alta | Inyección de datos inválidos, errores 500 (`DataTruncation`, `DateTimeParseException`). | Creación de DTOs y validadores `@Valid`. |
| **Subida de Archivos No Sanitizada / Path Traversal** | 🟡 Alta | Riesgo de sobreescritura de archivos arbitrarios o subida de ejecutables disfrazados. | Centralización en `FileStorageService` seguro. |
| **Ausencia de Pruebas Automatizadas** | 🟡 Media | Regresiones no detectadas al realizar refactorizaciones. | Creando suite de pruebas JUnit 5 + Mockito. |
| **Código Duplicado en Carga de Archivos** | 🟢 Baja | Dificultad de mantenimiento en controladores y servicios. | Refactorizado y unificado. |

---

## 2. Estrategia de Mitigación Aplicada

### 2.1. Gestión Segura de Credenciales y Secretos
- Ninguna clave de API ni contraseña permanece escrita directamente en el código fuente.
- Se utiliza el mecanismo estándar de Spring Boot de resolución de propiedades:
  `${NOMBRE_VAR:valor_por_defecto}`
- Se provee una plantilla clara y documentada en `.env.example`.

### 2.2. Migración a Spring Security 6 Estándar
- Creación de un `UserDetailsService` conectado a `UsuarioRepository`.
- Definición de roles formales: `ROLE_ADMIN` y `ROLE_USER`.
- Configuración de `SecurityFilterChain` con autorización declarativa por rutas:
  - Rutas públicas: catálogo, index, detalles, guía, transporte, mapa, recursos estáticos.
  - Rutas autenticadas: propuestas de contenido, publicación y edición de reseñas, panel de usuario.
  - Rutas de administración: `/admin/**` restringido exclusivamente a usuarios con rol `ADMIN`.
- Habilitación de protección CSRF en formularios web.

### 2.3. Almacenamiento Centralizado y Seguro de Archivos (`FileStorageService`)
- Sanitización estricta de nombres de archivo mediante UUIDs aleatorios y extensiones validadas por lista blanca (`.jpg`, `.jpeg`, `.png`, `.webp`).
- Verificación de tipo MIME real.
- Bloqueo de secuencias de escape de directorio (`..` o paths absolutos forzados).