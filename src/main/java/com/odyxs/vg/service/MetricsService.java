package com.odyxs.vg.service;

import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.LugarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);
    private static final int MAX_CHATBOT_REQ_PER_MIN = 20;
    private static final long WINDOW_MS = 60_000L;

    @Autowired(required = false)
    private LugarRepository lugarRepository;

    private final Map<String, Deque<Long>> rateLimitWindows = new ConcurrentHashMap<>();

    /**
     * Incrementa el contador de visitas de un lugar directamente en MySQL.
     */
    @Transactional
    public void registrarVisitaLugar(Long lugarId) {
        if (lugarRepository == null || lugarId == null) return;
        try {
            lugarRepository.findById(lugarId).ifPresent(lugar -> {
                long actuales = lugar.getVisitas() != null ? lugar.getVisitas() : 0L;
                lugar.setVisitas(actuales + 1L);
                lugarRepository.save(lugar);
            });
        } catch (Exception ex) {
            log.warn("No se pudo registrar visita en MySQL para lugar {}: {}", lugarId, ex.getMessage());
        }
    }

    /**
     * Obtiene el total de visitas acumuladas en MySQL para un lugar.
     */
    public long obtenerVisitas(Long lugarId) {
        if (lugarRepository == null || lugarId == null) return 0L;
        return lugarRepository.findById(lugarId)
                .map(l -> l.getVisitas() != null ? l.getVisitas() : 0L)
                .orElse(0L);
    }

    /**
     * Obtiene los lugares más visitados desde MySQL ordenados por visitas descendentes.
     */
    public List<Map<String, Object>> obtenerTopTrending(int limit) {
        if (lugarRepository == null || limit <= 0) return Collections.emptyList();
        return lugarRepository.findByEstadoOrderByVisitasDesc(Lugar.Estado.APROBADO)
                .stream()
                .limit(limit)
                .map(l -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("lugarId", l.getId());
                    item.put("nombre", l.getNombre());
                    item.put("puntosTrending", (double) (l.getVisitas() != null ? l.getVisitas() : 0L));
                    return item;
                })
                .toList();
    }

    /**
     * Rate limiter en memoria para peticiones al chatbot: máximo 20 peticiones por minuto por IP.
     */
    public boolean verificarLimiteRateChatbot(String ip) {
        if (ip == null || ip.isBlank()) return true;
        long ahora = System.currentTimeMillis();
        Deque<Long> timestamps = rateLimitWindows.computeIfAbsent(ip, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && (ahora - timestamps.peekFirst()) > WINDOW_MS) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= MAX_CHATBOT_REQ_PER_MIN) {
                return false;
            }
            timestamps.addLast(ahora);
            return true;
        }
    }
}
