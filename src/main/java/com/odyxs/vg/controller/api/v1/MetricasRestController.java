package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.repository.ChatbotLogRepository;
import com.odyxs.vg.service.MetricsService;
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
@Tag(name = "Métricas & Trending", description = "Contadores de visitas, ranking de lugares y estadísticas del asistente en MySQL")
public class MetricasRestController {

    @Autowired
    private MetricsService metricsService;

    @Autowired(required = false)
    private ChatbotLogRepository chatbotLogRepository;

    @Operation(summary = "Obtener Lugares Trending de Cartagena",
               description = "Devuelve los sitios turísticos más visitados ordenados por visitas acumuladas en MySQL.")
    @GetMapping("/trending")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> obtenerTrending(
            @RequestParam(defaultValue = "5") int limit) {
        List<Map<String, Object>> trending = metricsService.obtenerTopTrending(limit);
        return ResponseEntity.ok(ApiResponse.ok("Top " + limit + " trending places", trending));
    }

    @Operation(summary = "Registrar visita a un lugar turístico",
               description = "Incrementa el contador de visitas en MySQL.")
    @PostMapping("/visita/{lugarId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registrarVisita(@PathVariable Long lugarId) {
        metricsService.registrarVisitaLugar(lugarId);
        long total = metricsService.obtenerVisitas(lugarId);
        return ResponseEntity.ok(ApiResponse.ok("Visita registrada", Map.of("lugarId", lugarId, "totalVisitas", total)));
    }

    @Operation(summary = "Obtener visitas de un lugar")
    @GetMapping("/visitas/{lugarId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerVisitas(@PathVariable Long lugarId) {
        long total = metricsService.obtenerVisitas(lugarId);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("lugarId", lugarId, "totalVisitas", total)));
    }

    @Operation(summary = "Métricas globales del Asistente IA (MySQL)")
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
