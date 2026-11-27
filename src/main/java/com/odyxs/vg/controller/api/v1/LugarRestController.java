package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.dto.LugarCercanoDto;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.service.LugarGeoService;
import com.odyxs.vg.service.LugarService;
import com.odyxs.vg.service.MetricsService;
import com.odyxs.vg.service.SemanticVectorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/lugares")
@Tag(name = "Catálogo de Lugares (MySQL)", description = "Endpoints REST del catálogo oficial y geolocalización de Cartagena")
public class LugarRestController {

    @Autowired
    private LugarService lugarService;

    @Autowired
    private MetricsService metricsService;

    @Autowired
    private LugarGeoService lugarGeoService;

    @Autowired
    private SemanticVectorService semanticVectorService;

    @Operation(summary = "Listar lugares aprobados (con caché en memoria)",
               description = "Obtiene todos los sitios turísticos aprobados desde MySQL con caché en memoria.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Lugar>>> obtenerAprobados() {
        List<Lugar> lugares = lugarService.obtenerAprobados();
        return ResponseEntity.ok(ApiResponse.ok(lugares));
    }

    @Operation(summary = "Búsqueda Geoespacial: Lugares Cerca de Mí",
               description = "Devuelve los sitios turísticos más cercanos a las coordenadas del usuario en Cartagena usando fórmula Haversine sobre MySQL.")
    @GetMapping("/cerca-de-mi")
    public ResponseEntity<ApiResponse<List<LugarCercanoDto>>> buscarCercaDeMi(
            @Parameter(description = "Longitud GPS (ej. -75.5489)") @RequestParam double lng,
            @Parameter(description = "Latitud GPS (ej. 10.4233)") @RequestParam double lat,
            @Parameter(description = "Radio máximo de búsqueda en kilómetros") @RequestParam(defaultValue = "3.0") double radioKm) {

        List<LugarCercanoDto> cercanos = lugarGeoService.buscarLugaresCercanos(lng, lat, radioKm);
        return ResponseEntity.ok(ApiResponse.ok("Lugares cercanos en radio de " + radioKm + " km", cercanos));
    }

    @Operation(summary = "Obtener detalle de un lugar por ID",
               description = "Devuelve la información de un sitio aprobado y registra automáticamente una visita en MySQL.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Lugar>> obtenerPorId(@PathVariable Long id) {
        Lugar lugar = lugarService.obtenerPorId(id);
        if (lugar == null || lugar.getEstado() != Lugar.Estado.APROBADO) {
            return ResponseEntity.notFound().build();
        }
        metricsService.registrarVisitaLugar(id);
        return ResponseEntity.ok(ApiResponse.ok(lugar));
    }

    @Operation(summary = "Listar lugares por categoría")
    @GetMapping("/categoria/{categoriaId}")
    public ResponseEntity<ApiResponse<List<Lugar>>> obtenerPorCategoria(@PathVariable Long categoriaId) {
        List<Lugar> lugares = lugarService.obtenerPorCategoriaAprobados(categoriaId);
        return ResponseEntity.ok(ApiResponse.ok(lugares));
    }

    @Operation(summary = "Búsqueda de lugares por texto")
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<List<Lugar>>> buscar(@RequestParam String q) {
        List<Lugar> resultados = lugarService.buscar(q);
        return ResponseEntity.ok(ApiResponse.ok(resultados));
    }

    @Operation(summary = "Búsqueda Vectorial Semántica (Embeddings + Similitud del Coseno v4.0)",
               description = "Comprende la intención natural del viajero mediante vectores L2-normalizados y similitud del coseno.")
    @GetMapping("/semantico")
    public ResponseEntity<ApiResponse<List<SemanticVectorService.ResultadoSemanticoDto>>> buscarSemantico(
            @Parameter(description = "Consulta en lenguaje natural") @RequestParam String q,
            @Parameter(description = "Cantidad máxima de resultados (1..12)") @RequestParam(defaultValue = "5") int topK) {
        List<SemanticVectorService.ResultadoSemanticoDto> resultados = semanticVectorService.buscarSemanticamente(q, topK);
        return ResponseEntity.ok(ApiResponse.ok("Resultados ordenados por Similitud del Coseno vectorial", resultados));
    }
}
