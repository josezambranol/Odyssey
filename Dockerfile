# ── Etapa 1: Runtime Optimizado con Java 21 LTS ─────────────────
FROM eclipse-temurin:21-jre-alpine AS runner

LABEL maintainer="Equipo Odyssey <soporte@odyssey.com>"
LABEL description="Odyssey - Guía Turística & Asistente IA para Cartagena de Indias"

# Instalar curl para healthchecks y herramientas de diagnóstico
RUN apk add --no-cache curl tzdata

# Configurar zona horaria de Colombia
ENV TZ=America/Bogota

# Crear usuario y grupo de seguridad sin privilegios de root
RUN addgroup -g 10001 -S odyssey && \
    adduser -u 10001 -S odyssey -G odyssey

WORKDIR /app

# Crear directorio para uploads de imágenes y asignar permisos
RUN mkdir -p /app/uploads/lugares /app/uploads/actividades /app/uploads/eventos && \
    chown -R odyssey:odyssey /app

# Copiar el ejecutable empaquetado de Spring Boot
COPY target/odyssey-0.0.1-SNAPSHOT.jar /app/odyssey.jar
RUN chown odyssey:odyssey /app/odyssey.jar

# Cambiar al usuario sin privilegios
USER odyssey:odyssey

# Exponer el puerto de la aplicación
EXPOSE 8080

# Variables de entorno JVM para contenedores
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

# Healthcheck del contenedor
HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8080/v3/api-docs || exit 1

# Comando de arranque
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/odyssey.jar"]
