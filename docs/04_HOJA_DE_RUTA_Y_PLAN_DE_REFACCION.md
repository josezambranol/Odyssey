# 🚀 ODYSSEY — Hoja de Ruta y Plan de Refactorización

## Fases de Modernización

### Fase 1: Blindaje de Seguridad y Configuración (En Curso)
- [x] Extraer secretos y API keys a variables de entorno (`.env`).
- [x] Agregar dependencia `spring-boot-starter-validation` en `pom.xml`.
- [ ] Implementar `FileStorageService` unificado y seguro.
- [ ] Implementar `CustomUserDetailsService` y `SecurityConfig` estándar de Spring Security 6.
- [ ] Configurar formularios de login y registro compatibles con Thymeleaf y Spring Security.

### Fase 2: Robustez de Backend (DTOs, Validaciones y Manejo Global de Errores)
- [ ] Crear DTOs para solicitudes de registro, login, creación y edición de lugares, eventos y actividades.
- [ ] Crear `@ControllerAdvice` con `@ExceptionHandler` para respuestas de error elegantes (404, 403, 500, `MaxUploadSizeExceeded`).
- [ ] Desacoplar lógica de controladores y moverla a servicios de dominio.

### Fase 3: Pruebas Automatizadas y Calidad de Código
- [ ] Configurar suite de pruebas unitarias con JUnit 5 y Mockito para servicios (`UsuarioServiceTest`, `LugarServiceTest`, `ResenaServiceTest`).
- [ ] Configurar pruebas de integración para controladores con `@WebMvcTest` y `spring-security-test`.

### Fase 4: Optimización, Paginación y Preparación para Nuevos Módulos
- [ ] Añadir paginación con `Pageable` en listados de lugares y reseñas.
- [ ] Preparación arquitectónica para renombre de marca y posterior rediseño de frontend.