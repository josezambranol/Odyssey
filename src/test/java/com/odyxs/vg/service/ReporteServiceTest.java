package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.Reporte;
import com.odyxs.vg.repository.mongo.ReporteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private ReporteRepository reporteRepository;

    @InjectMocks
    private ReporteService reporteService;

    @Test
    void guardar_GeneraCodigoSiEstaVacio() {
        Reporte reporte = new Reporte();
        reporte.setTitulo("Incidencia en Murallas");
        reporte.setTipo(Reporte.TipoReporte.INCIDENCIA_TRANSPORTE);

        when(reporteRepository.save(any(Reporte.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reporte guardado = reporteService.guardar(reporte);

        assertNotNull(guardado);
        assertNotNull(guardado.getCodigo());
        assertTrue(guardado.getCodigo().startsWith("REP-"));
        verify(reporteRepository, times(1)).save(reporte);
    }

    @Test
    void listarPaginado_ConFiltros_UsaMetodoOptimizado() {
        Pageable pageable = PageRequest.of(0, 10);
        Reporte r = new Reporte();
        r.setTipo(Reporte.TipoReporte.VISITA_TURISTICA);
        r.setEstado(Reporte.EstadoReporte.NUEVO);

        Page<Reporte> mockPage = new PageImpl<>(List.of(r), pageable, 1);
        when(reporteRepository.findByTipoAndEstado(Reporte.TipoReporte.VISITA_TURISTICA, Reporte.EstadoReporte.NUEVO, pageable))
                .thenReturn(mockPage);

        Page<Reporte> res = reporteService.listarPaginado(Reporte.TipoReporte.VISITA_TURISTICA, Reporte.EstadoReporte.NUEVO, pageable);

        assertNotNull(res);
        assertEquals(1, res.getTotalElements());
        assertEquals(Reporte.TipoReporte.VISITA_TURISTICA, res.getContent().get(0).getTipo());
        verify(reporteRepository, times(1)).findByTipoAndEstado(Reporte.TipoReporte.VISITA_TURISTICA, Reporte.EstadoReporte.NUEVO, pageable);
    }

    @Test
    void obtenerEstadisticasGlobales_RetornaAgregaciones() {
        when(reporteRepository.count()).thenReturn(15000L);
        when(reporteRepository.contarAgrupadoPorTipo()).thenReturn(Map.of("VISITA_TURISTICA", 8000L, "CONSULTA_IA", 7000L));
        when(reporteRepository.contarAgrupadoPorEstado()).thenReturn(Map.of("RESUELTO", 14000L, "NUEVO", 1000L));
        when(reporteRepository.obtenerTopEntidadesConMasReportes(5)).thenReturn(List.of(Map.of("nombreEntidad", "Castillo San Felipe", "total", 4500L)));

        Map<String, Object> stats = reporteService.obtenerEstadisticasGlobales();

        assertNotNull(stats);
        assertEquals(15000L, stats.get("totalReportes"));
        assertTrue(stats.containsKey("conteoPorTipo"));
        assertTrue(stats.containsKey("conteoPorEstado"));
        assertTrue(stats.containsKey("topLugaresReportados"));
    }

    @Test
    void buscarPorId_Encontrado() {
        Reporte reporte = new Reporte();
        reporte.setId("mongo_id_123");
        when(reporteRepository.findById("mongo_id_123")).thenReturn(Optional.of(reporte));

        Optional<Reporte> res = reporteService.buscarPorId("mongo_id_123");

        assertTrue(res.isPresent());
        assertEquals("mongo_id_123", res.get().getId());
    }
}
