package com.odyxs.vg.service;

import com.odyxs.vg.entity.Reporte;
import com.odyxs.vg.repository.ReporteRepository;
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
        when(reporteRepository.count()).thenReturn(150L);
        when(reporteRepository.contarAgrupadoPorTipoRaw()).thenReturn(List.<Object[]>of(
                new Object[]{Reporte.TipoReporte.VISITA_TURISTICA, 80L},
                new Object[]{Reporte.TipoReporte.CONSULTA_IA, 70L}
        ));
        when(reporteRepository.contarAgrupadoPorEstadoRaw()).thenReturn(List.<Object[]>of(
                new Object[]{Reporte.EstadoReporte.RESUELTO, 140L},
                new Object[]{Reporte.EstadoReporte.NUEVO, 10L}
        ));
        when(reporteRepository.obtenerTopEntidadesRaw(any(Pageable.class))).thenReturn(List.<Object[]>of(
                new Object[]{1L, "Castillo San Felipe", 45L}
        ));

        Map<String, Object> stats = reporteService.obtenerEstadisticasGlobales();

        assertNotNull(stats);
        assertEquals(150L, stats.get("totalReportes"));
        assertTrue(stats.containsKey("conteoPorTipo"));
        assertTrue(stats.containsKey("conteoPorEstado"));
        assertTrue(stats.containsKey("topLugaresReportados"));
    }

    @Test
    void buscarPorId_Encontrado() {
        Reporte reporte = new Reporte();
        reporte.setId("rep_id_123");
        when(reporteRepository.findById("rep_id_123")).thenReturn(Optional.of(reporte));

        Optional<Reporte> res = reporteService.buscarPorId("rep_id_123");

        assertTrue(res.isPresent());
        assertEquals("rep_id_123", res.get().getId());
    }
}
