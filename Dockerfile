# ══════════════════════════════════════════════════════════════════
# ETAPA 1: Compilación Reproducible con Maven 3.9 + JDK 21 LTS
# ══════════════════════════════════════════════════════════════════
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ══════════════════════════════════════════════════════════════════
# ETAPA 2: Runtime Optimizado con JRE 21 Alpine + Virtual Threads
# ══════════════════════════════════════════════════════════════════
FROM eclipse-temurin:21-jre-alpine AS runner

LABEL maintainer="Equipo Odyssey <soporte@odyssey.com>"
LABEL description="Odyssey 5.0.0 - Smart Destination: Visión Multimodal, RAG Vectorial, Pases QR HMAC & PWA"

RUN apk add --no-cache curl tzdata

ENV TZ=America/Bogota
ENV ODYSSEY_UPLOAD_DIR=/app/uploads

RUN addgroup -g 10001 -S odyssey && \
    adduser -u 10001 -S -h /home/odyssey -G odyssey odyssey

WORKDIR /app

RUN mkdir -p /app/uploads/lugares /app/uploads/actividades /app/uploads/eventos /home/odyssey && \
    chown -R odyssey:odyssey /app /home/odyssey

COPY --from=builder --chown=odyssey:odyssey /build/target/odyssey-*.jar /app/odyssey.jar

USER odyssey:odyssey

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://localhost:${PORT:-8080}/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/odyssey.jar"]
