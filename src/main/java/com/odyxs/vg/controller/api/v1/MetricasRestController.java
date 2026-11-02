package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.repository.mongo.ChatbotLogRepository;
import com.odyxs.vg.service.RedisMetricsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metricas")
@Tag(name = "Métricas & Trending Redis", description = "Telemetría en memoria, contadores atómicos y Leaderboard en tiempo real con Redis")
public class MetricasRestController {

    @Autowired
    private RedisMetricsService redisMetricsService;

    @Autowired(required = false)
    private ChatbotLogRepository chatbotLogRepository;

    @Operation(summary = "Obtener Lugares Trending de Cartagena (Redis Sorted Set)",
               description = "Devuelve los sitios turísticos más populares en tiempo real calculados con ZINCRBY en Redis.")
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerTrending(
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> trending = redisMetricsService.obtenerTopTrending(limit);
        return ResponseEntity.ok(ApiResponse.ok("Top " + limit + " trending places", trending));
    }

    @Operation(summary = "Registrar visita a un lugar turístico",
               description = "Incrementa atómicamente el contador en Redis sin sobrecargar MySQL.")
    @PostMapping("/visita/{lugarId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarVisita(@PathVariable Long lugarId) {
        redisMetricsService.registrarVisitaLugar(lugarId);
        long total = redisMetricsService.obtenerVisitas(lugarId);
        return ResponseEntity.ok(ApiResponse.ok("Visita registrada", Map.of("lugarId", lugarId, "totalVisitas", total)));
    }

    @Operation(summary = "Obtener visitas en memoria de un lugar")
    @GetMapping("/visitas/{lugarId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerVisitas(@PathVariable Long lugarId) {
        long total = redisMetricsService.obtenerVisitas(lugarId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("lugarId", lugarId, "totalVisitas", total)));
    }

    @Operation(summary = "Métricas globales del Asistente Gemini (MongoDB Telemetry)")
    @GetMapping("/chatbot-stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerChatbotStats() {
        Map<String, Object> stats = new HashMap<>();
        if (chatbotLogRepository != null) {
            stats.put("totalConsultas", chatbotLogRepository.count());
            stats.put("consultasExitosas", chatbotLogRepository.countByExitoTrue());
            stats.put("consultasConFallo", chatbotLogRepository.countByExitoFalse());
        } else {
            stats.put("totalConsultas", 0);
        }
        return ResponseEntity.ok(ApiResponse.ok("Estadísticas de chatbot", stats));
    }
}
