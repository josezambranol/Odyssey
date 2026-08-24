# 📖 ODYSSEY — Contexto del Proyecto e Historias de Usuario

## 1. Contexto General del Proyecto
**ODYSSEY (Cartagena Travel Guide & Assistant)** es una plataforma web integral desarrollada a lo largo de los semestres 4° a 7° de la carrera de Tecnología en Desarrollo de Software (TDS). Su propósito fundamental es ofrecer una guía turística interactiva, confiable, bilingüe e impulsada por Inteligencia Artificial para la ciudad de **Cartagena de Indias, Colombia** (Patrimonio Histórico y Cultural de la Humanidad UNESCO).

La plataforma permite a los turistas descubrir sitios históricos, playas, gastronomía, eventos y actividades, interactuar mediante un asistente virtual con IA (ODYSSEY / Google Gemini), emitir reseñas con calificaciones, y a los emprendedores locales proponer nuevos lugares y actividades para su debida moderación administrativa.

---

## 2. Actores / Roles del Sistema

| Rol | Descripción | Capacidades Clave |
| :--- | :--- | :--- |
| **Turista / Visitante No Registrado** | Usuario general que consulta información pública. | Consulta catálogo, mapa interactivo, guía turística, conversor de idioma y asistente básico. |
| **Usuario Registrado / Emprendedor Local** | Turista registrado o prestador de servicios turísticos locales. | Publica reseñas (1 a 5 estrellas), propone nuevos lugares, eventos y actividades para aprobación. |
| **Administrador (`ROLE_ADMIN`)** | Gestor general de la plataforma ODYSSEY. | Aprueba/rechaza propuestas de lugares, eventos y actividades; gestiona el catálogo oficial y categorías. |

---

## 3. Matriz de Requisitos Funcionales (Historias de Usuario)

Basado en el levantamiento metodológico oficial del proyecto (**Anexo 4**):

| ID | Historia de Usuario | Criterios de Aceptación / Estado |
| :--- | :--- | :--- |
| **RF001** | **Gestión de Acceso:** *Como usuario/administrador quiero registrarme e iniciar sesión para garantizar un acceso seguro y personalizado.* | Formulario de registro y login con cifrado BCrypt; autenticación y control de roles. |
| **RF002** | **Detalle de Sitios Turísticos:** *Como administrador quiero configurar los detalles de cada sitio turístico (horarios, tarifas, descripción, ubicación).* | Mapeo detallado de lugares con imágenes, mapas de Google, descripción y categorías. |
| **RF003** | **Categorización:** *Como turista quiero ver los lugares organizados por categoría para explorar de forma rápida e intuitiva.* | Filtros por categorías (Playas, Museos, Cines, Hoteles, Sitios Históricos, Gastronomía, Festivales). |
| **RF004** | **Edición de Contenido:** *Como emprendedor/administrador quiero actualizar los datos de mi lugar turístico para mantener vigente la oferta.* | Panel de edición de lugares, eventos y actividades con sustitución segura de imágenes. |
| **RF005** | **Enlaces Externos / Reservas:** *Como usuario quiero ser redirigido al sitio oficial o mapa del lugar para reservas y geolocalización.* | Integración de enlaces a Google Maps y sitios web oficiales de prestadores. |
| **RF006** | **Propuesta de Negocios Locales:** *Como emprendedor quiero registrar mi negocio en la guía para aumentar mi visibilidad turística.* | Formulario `/proponer-lugar`, `/proponer-actividad` y `/proponer-evento` con estado `PENDIENTE`. |
| **RF007** | **Reseñas y Calificaciones:** *Como usuario registrado quiero agregar y editar reseñas con puntuación de 1 a 5 estrellas.* | Módulo de reseñas por lugar con validación de autoría para editar/eliminar. |
| **RF008** | **Mapa Interactivo:** *Como usuario quiero visualizar un mapa interactivo con la ubicación de los puntos turísticos clave.* | Módulo `/mapa` con visualización geográfica de puntos turísticos de Cartagena. |
| **RF009** | **Búsqueda Global y Filtrado:** *Como usuario quiero buscar sitios turísticos por palabras clave y categorías.* | Endpoint `/buscar?q=...` con búsqueda insensible a mayúsculas/minúsculas. |
| **RF010** | **Ficha Informativa Completa:** *Como usuario quiero ver fotos, descripción, clima en tiempo real e información turística.* | Detalle de lugar `/lugares/{id}`, widget de clima en tiempo real con Open-Meteo API. |
| **RF011** | **Panel de Moderación y Administración:** *Como administrador quiero aprobar, rechazar o eliminar contenido propuesto.* | Panel de administración `/admin` con flujos de aprobación y rechazo en 1 clic. |

---

## 4. Requisitos No Funcionales (RNF)

- **RNF01 (Seguridad):** Cifrado de contraseñas con algoritmo robusto (BCrypt), protección contra CSRF y control de acceso basado en roles (RBAC).
- **RNF02 (Internacionalización - i18n):** Interfaz y respuestas completamente localizadas en Español (ES) e Inglés (EN).
- **RNF03 (Disponibilidad y Resiliencia):** Arquitectura de Chatbot con fallback automático a base de datos si la API de IA no está disponible.
- **RNF04 (Persistencia de Archivos):** Almacenamiento desacoplado del ciclo de vida del empaquetado JAR (`~/odyssey-uploads/`).
- **RNF05 (Rendimiento):** Consultas indexadas y paginación para escalabilidad de registros.