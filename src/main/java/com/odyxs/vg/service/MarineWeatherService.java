package com.odyxs.vg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Servicio de Inteligencia Climática y Marina Costera (Odyssey v4.0).
 * Consulta Open-Meteo para Cartagena de Indias (10.3910, -75.4794) con caché TTL en memoria (15 min)
 * y fallback climatológico resiliente cuando no hay salida a internet.
 */
@Service
public class MarineWeatherService {

    private static final Logger log = LoggerFactory.getLogger(MarineWeatherService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final ZoneId ZONA_CARTAGENA = ZoneId.of("America/Bogota");

    private static final String OPEN_METEO_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=10.3910&longitude=-75.4794"
            + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,wind_gusts_10m,uv_index,weather_code"
            + "&timezone=America%2FBogota";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicReference<CachedReport> cache = new AtomicReference<>();

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private record CachedReport(ReporteClimaMarinoDto reporte, Instant expiraEn) {}

    public record ReporteClimaMarinoDto(
            double temperaturaC,
            double sensacionTermicaC,
            int humedadRelativa,
            double vientoKmh,
            double rafagasKmh,
            double vientoNudos,
            double indiceUv,
            String nivelUv,
            String semaforoNautico,
            String estadoZarpeIslas,
            String recomendacionViajero,
            String mejorFranjaHoraria,
            String fuenteDatos,
            String actualizadoEn
    ) {}

    public ReporteClimaMarinoDto obtenerPronosticoActual() {
        CachedReport actual = cache.get();
        if (actual != null && Instant.now().isBefore(actual.expiraEn())) {
            registrarMetrica("cache_hit");
            return actual.reporte();
        }

        ReporteClimaMarinoDto fresco = consultarOpenMeteo();
        if (fresco != null) {
            cache.set(new CachedReport(fresco, Instant.now().plus(CACHE_TTL)));
            registrarMetrica("live_api");
            return fresco;
        }

        ReporteClimaMarinoDto respaldo = construirRespaldoClimatologico();
        cache.set(new CachedReport(respaldo, Instant.now().plus(Duration.ofMinutes(3))));
        registrarMetrica("fallback_local");
        return respaldo;
    }

    private ReporteClimaMarinoDto consultarOpenMeteo() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OPEN_METEO_URL))
                    .timeout(Duration.ofMillis(3500))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) return null;

            JsonNode current = objectMapper.readTree(resp.body()).path("current");
            if (current.isMissingNode()) return null;

            double temp = redondear1(current.path("temperature_2m").asDouble(29.5));
            double sensacion = redondear1(current.path("apparent_temperature").asDouble(33.0));
            int humedad = current.path("relative_humidity_2m").asInt(78);
            double viento = redondear1(current.path("wind_speed_10m").asDouble(18.0));
            double rafagas = redondear1(current.path("wind_gusts_10m").asDouble(viento * 1.25));
            double uv = redondear1(current.path("uv_index").asDouble(estimarUvPorHora()));

            return evaluarCondicionesMarinas(temp, sensacion, humedad, viento, rafagas, uv, "Open-Meteo Live Marine & Weather");
        } catch (Exception ex) {
            log.debug("Clima marino usando respaldo climatológico local: {}", ex.getMessage());
            return null;
        }
    }

    public ReporteClimaMarinoDto evaluarCondicionesMarinas(
            double temp, double sensacion, int humedad, double vientoKmh, double rafagasKmh, double uv, String fuente) {

        double nudos = redondear1(vientoKmh * 0.539957);
        String nivelUv = clasificarUv(uv);

        String semaforo;
        String estadoZarpe;
        if (vientoKmh < 22.0 && rafagasKmh < 34.0) {
            semaforo = "VERDE";
            estadoZarpe = "Condiciones óptimas de navegación hacia Islas del Rosario, Barú y Tierra Bomba.";
        } else if (vientoKmh <= 32.0) {
            semaforo = "AMARILLO";
            estadoZarpe = "Brisa moderada (" + vientoKmh + " km/h). Zarpa desde Muelle de la Bodeguita antes de las 9:30 AM; retorno sugerido antes de las 3:30 PM.";
        } else {
            semaforo = "ROJO";
            estadoZarpe = "Viento alisio fuerte (" + vientoKmh + " km/h). Posible restricción de Capitanía de Puerto para embarcaciones menores; prioriza Ciudad Amurallada y museos.";
        }

        String recomendacion = construirRecomendacion(semaforo, uv, sensacion);
        String mejorFranja = (uv >= 8.0)
                ? "07:00–10:30 AM y 04:30–06:30 PM (atardecer en murallas)"
                : "Todo el día con hidratación constante";

        return new ReporteClimaMarinoDto(
                temp,
                sensacion,
                humedad,
                vientoKmh,
                rafagasKmh,
                nudos,
                uv,
                nivelUv,
                semaforo,
                estadoZarpe,
                recomendacion,
                mejorFranja,
                fuente,
                ZonedDateTime.now(ZONA_CARTAGENA).toOffsetDateTime().toString()
        );
    }

    private ReporteClimaMarinoDto construirRespaldoClimatologico() {
        double uvEstimado = estimarUvPorHora();
        return evaluarCondicionesMarinas(
                29.4, 33.2, 78, 17.5, 24.0, uvEstimado,
                "Modelo Climatológico Costero Odyssey (Modo Resiliente)"
        );
    }

    private double estimarUvPorHora() {
        int hora = ZonedDateTime.now(ZONA_CARTAGENA).getHour();
        if (hora < 6 || hora >= 18) return 0.0;
        if (hora >= 11 && hora <= 14) return 10.5;
        if (hora >= 9 && hora <= 16) return 7.5;
        return 3.5;
    }

    private static String clasificarUv(double uv) {
        if (uv >= 11.0) return "EXTREMO";
        if (uv >= 8.0) return "MUY_ALTO";
        if (uv >= 6.0) return "ALTO";
        if (uv >= 3.0) return "MODERADO";
        return "BAJO";
    }

    private static String construirRecomendacion(String semaforo, double uv, double sensacion) {
        StringBuilder sb = new StringBuilder();
        if ("ROJO".equals(semaforo)) {
            sb.append("Evita lanchas rápidas en mar abierto hoy; recorre el Castillo San Felipe, Museo del Oro Zenú o Getsemaní. ");
        } else if ("AMARILLO".equals(semaforo)) {
            sb.append("Para islas, elige embarcaciones certificadas en Muelle de la Bodeguita temprano en la mañana. ");
        } else {
            sb.append("Excelente día para snorkel en Islas del Rosario, kayak en La Boquilla o caminata colonial. ");
        }
        if (uv >= 8.0) {
            sb.append("Radiación UV elevada: usa protector SPF 50+, sombrero y agua fresca (sensación de ").append(sensacion).append(" °C).");
        } else {
            sb.append("Sensación térmica agradable de ").append(sensacion).append(" °C para recorridos a pie.");
        }
        return sb.toString();
    }

    private void registrarMetrica(String origen) {
        if (meterRegistry != null) {
            meterRegistry.counter("odyssey.marine_weather.requests", "source", origen).increment();
        }
    }

    private static double redondear1(double val) {
        return Math.round(val * 10.0) / 10.0;
    }
}
