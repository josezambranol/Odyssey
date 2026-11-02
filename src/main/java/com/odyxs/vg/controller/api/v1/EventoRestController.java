package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.service.EventoSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/eventos")
@Tag(name = "Cartelera de Eventos Culturales", description = "Sincronización en vivo, web scraping de agendas externas y consulta de eventos en Cartagena")
public class EventoRestController {

    @Autowired
    private EventoSyncService eventoSyncService;

    @Autowired
    private EventoRepository eventoRepository;

    @Operation(summary = "🌐 Sincronizar eventos con Web Scraping de fuentes externas",
               description = "Rastrea agendas culturales de Cartagena en tiempo real, deduplica y guarda nuevos eventos en MySQL y auditoría en MongoDB.")
    @PostMapping("/sync-externo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sincronizarExterno() {
        Map<String, Object> resultado = eventoSyncService.sincronizarEventosExternos();
        return ResponseEntity.ok(ApiResponse.ok("Sincronización externa completada", resultado));
    }

    @Operation(summary = "Obtener cartelera clasificada (Hoy, Próximos, Finalizados)",
               description = "Devuelve los eventos clasificados con caché de alta velocidad en Redis.")
    @GetMapping("/clasificados")
    public ResponseEntity<ApiResponse<Map<String, List<Evento>>>> obtenerClasificados() {
        Map<String, List<Evento>> clasificados = eventoSyncService.obtenerEventosClasificados();
        return ResponseEntity.ok(ApiResponse.ok("Eventos clasificados de Cartagena", clasificados));
    }

    @Operation(summary = "Listar eventos activos paginados")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Evento>>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fecha"));
        Page<Evento> eventos = eventoRepository.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Eventos activos", eventos.getContent()));
    }
}
