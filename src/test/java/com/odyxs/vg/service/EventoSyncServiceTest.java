package com.odyxs.vg.service;

import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.repository.EventoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoSyncServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @InjectMocks
    private EventoSyncService eventoSyncService;

    @Test
    void obtenerEventosClasificados_AgrupaHoyProximosYFinalizados() {
        Evento eHoy = new Evento();
        eHoy.setNombre("Festival Hoy");
        eHoy.setFecha(LocalDate.now());

        when(eventoRepository.findByActivoTrueAndFecha(any(LocalDate.class))).thenReturn(List.of(eHoy));
        when(eventoRepository.findByActivoTrueAndFechaGreaterThanOrderByFechaAsc(any(LocalDate.class))).thenReturn(List.of());
        when(eventoRepository.findByActivoTrueAndFechaLessThanOrderByFechaDesc(any(LocalDate.class))).thenReturn(List.of());

        Map<String, List<Evento>> res = eventoSyncService.obtenerEventosClasificados();

        assertNotNull(res);
        assertEquals(1, res.get("hoy").size());
        assertEquals("Festival Hoy", res.get("hoy").get(0).getNombre());
    }

    @Test
    void refrescarCarteleraEventos_RetornaTotalActivosDesdeMySQL() {
        when(eventoRepository.findByActivoTrueOrderByFechaAsc()).thenReturn(List.of(new Evento(), new Evento()));

        Map<String, Object> res = eventoSyncService.refrescarCarteleraEventos();

        assertEquals("SUCCESS", res.get("status"));
        assertEquals(2L, ((Number) res.get("totalEventosActivos")).longValue());
        verify(eventoRepository, times(1)).findByActivoTrueOrderByFechaAsc();
    }
}
