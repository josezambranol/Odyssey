package com.odyxs.vg.service;

import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.repository.EventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class EventoSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventoSyncService.class);

    @Autowired
    private EventoRepository eventoRepository;

    /**
     * Obtiene los eventos activos clasificados en Hoy, Próximos y Finalizados desde MySQL.
     */
    @Cacheable(value = "eventosClasificados", key = "'agenda'")
    public Map<String, List<Evento>> obtenerEventosClasificados() {
        LocalDate hoy = LocalDate.now();
        Map<String, List<Evento>> mapa = new LinkedHashMap<>();

        mapa.put("hoy", eventoRepository.findByActivoTrueAndFecha(hoy));
        mapa.put("proximos", eventoRepository.findByActivoTrueAndFechaGreaterThanOrderByFechaAsc(hoy));
        mapa.put("finalizados", eventoRepository.findByActivoTrueAndFechaLessThanOrderByFechaDesc(hoy));

        return mapa;
    }

    /**
     * Refresca la caché de cartelera de eventos culturales en MySQL.
     */
    @CacheEvict(value = "eventosClasificados", allEntries = true)
    public Map<String, Object> refrescarCarteleraEventos() {
        long totalActivos = eventoRepository.findByActivoTrueOrderByFechaAsc().size();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("status", "SUCCESS");
        resultado.put("mensaje", "Cartelera de eventos actualizada desde MySQL.");
        resultado.put("totalEventosActivos", totalActivos);
        resultado.put("fechaSincronizacion", LocalDate.now().toString());
        log.info("[EVENTO_SYNC] Cartelera refrescada desde MySQL. Total activos: {}", totalActivos);
        return resultado;
    }
}
