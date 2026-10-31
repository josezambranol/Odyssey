package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.common.PageResponse;
import com.odyxs.vg.domain.mongo.Reporte;
import com.odyxs.vg.dto.ReporteResumenDto;
import com.odyxs.vg.service.ReporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes & Analítica NoSQL", description = "Endpoints de alta escala en MongoDB para gestión de +15,000 documentos, analítica agregada y benchmark")
public class ReporteRestController {

    @Autowired
    private ReporteService reporteService;

    @Operation(summary = "Listar reportes paginados (Optimizado para +15,000 registros)",
               description = "Devuelve DTOs de proyección livianos con ordenamiento y filtros por tipo o estado. Latencia < 30ms.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReporteResumenDto>>> listar(
            @Parameter(description = "Tipo de reporte") @RequestParam(required = false) Reporte.TipoReporte tipo,
            @Parameter(description = "Estado del reporte") @RequestParam(required = false) Reporte.EstadoReporte estado,
            @Parameter(description = "Número de página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "fechaCreacion") String sort,
            @Parameter(description = "Dirección (asc/desc)") @RequestParam(defaultValue = "desc") String dir) {

        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));

        Page<Reporte> resultado = reporteService.listarPaginado(tipo, estado, pageable);
        Page<ReporteResumenDto> dtos = resultado.map(ReporteResumenDto::new);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(dtos)));
    }

    @Operation(summary = "Búsqueda por texto en reportes", description = "Aprovecha el índice de texto nativo de MongoDB.")
    @GetMapping("/buscar")
    public ResponseEntity<ApiResponse<PageResponse<ReporteResumenDto>>> buscar(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Reporte> resultado = reporteService.buscarTexto(q, pageable);
        Page<ReporteResumenDto> dtos = resultado.map(ReporteResumenDto::new);
        return ResponseEntity.ok(ApiResponse.ok(new PageResponse<>(dtos)));
    }

    @Operation(summary = "Obtener métricas y agregaciones globales",
               description = "Calcula métricas analíticas en tiempo real usando Aggregation Pipelines de MongoDB, con caché en Redis.")
    @GetMapping("/metricas")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerMetricas() {
        return ResponseEntity.ok(ApiResponse.ok(reporteService.obtenerEstadisticasGlobales()));
    }

    @Operation(summary = "📥 Exportación Masiva en Streaming CSV (+15,000 registros)",
               description = "Descarga de alto rendimiento con cursor directo a MongoDB y StreamingResponseBody (consumo de RAM < 20MB).")
    @GetMapping(value = "/exportar/csv", produces = "text/csv; charset=utf-8")
    public ResponseEntity<org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody> exportarCsv(
            @RequestParam(required = false) Reporte.TipoReporte tipo) {

        org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody stream =
                outputStream -> reporteService.exportarReportesCsvStreaming(outputStream, tipo);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"odyssey_reportes_export_" + java.time.LocalDate.now() + ".csv\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("text/csv; charset=utf-8"))
                .body(stream);
    }

    @Operation(summary = "🚀 Benchmark masivo: Generar 15,000 reportes en MongoDB",
               description = "Inserta 15,000 documentos sintéticos en lotes con mongoTemplate.insertAll() y devuelve métricas de rendimiento y latencia en vivo.")
    @PostMapping("/benchmark/seed-15k")
    public ResponseEntity<ApiResponse<Map<String, Object>>> benchmark15k() {
        Map<String, Object> stats = reporteService.ejecutarBenchmark15000Documentos();
        return ResponseEntity.ok(ApiResponse.ok("Benchmark ejecutado con éxito", stats));
    }

    @Operation(summary = "Obtener detalle completo de un reporte por ID (MongoDB ObjectId)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Reporte>> obtenerPorId(@PathVariable String id) {
        return reporteService.buscarPorId(id)
                .map(r -> ResponseEntity.ok(ApiResponse.ok(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Crear nuevo reporte de actividad o incidencia")
    @PostMapping
    public ResponseEntity<ApiResponse<Reporte>> crear(@RequestBody Reporte reporte) {
        Reporte guardado = reporteService.guardar(reporte);
        return ResponseEntity.ok(ApiResponse.ok("Reporte registrado con éxito", guardado));
    }
}
