package com.odyxs.vg.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
public class RedisMetricsService {

    private static final Logger log = LoggerFactory.getLogger(RedisMetricsService.class);

    private static final String KEY_VISITAS_PREFIX = "visitas:lugar:";
    private static final String KEY_TRENDING_LUGARES = "trending:lugares:cartagena";
    private static final String KEY_RATE_LIMIT_PREFIX = "ratelimit:ai:";

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Incrementa atómicamente el contador de visitas de un lugar en Redis y actualiza el Leaderboard en tiempo real.
     */
    public void registrarVisitaLugar(Long lugarId) {
        if (redisTemplate == null || lugarId == null) return;
        try {
            // 1. Contador atómico simple de visitas
            redisTemplate.opsForValue().increment(KEY_VISITAS_PREFIX + lugarId);

            // 2. Ranking / Leaderboard en tiempo real usando Redis Sorted Set (ZSET)
            redisTemplate.opsForZSet().incrementScore(KEY_TRENDING_LUGARES, String.valueOf(lugarId), 1.0);
        } catch (Exception ex) {
            log.warn("Redis no disponible para registrar visita: {}", ex.getMessage());
        }
    }

    /**
     * Obtiene el total de visitas acumuladas en memoria para un lugar.
     */
    public long obtenerVisitas(Long lugarId) {
        if (redisTemplate == null || lugarId == null) return 0L;
        try {
            Object val = redisTemplate.opsForValue().get(KEY_VISITAS_PREFIX + lugarId);
            if (val instanceof Number) return ((Number) val).longValue();
            if (val != null) return Long.parseLong(val.toString());
        } catch (Exception ex) {
            log.warn("Error leyendo visitas de Redis: {}", ex.getMessage());
        }
        return 0L;
    }

    /**
     * Obtiene los IDs de los lugares más visitados (Trending Topics) en tiempo real desde Redis.
     */
    public List<Map<String, Object>> obtenerTopTrending(int limit) {
        if (redisTemplate == null) return Collections.emptyList();
        try {
            Set<ZSetOperations.TypedTuple<Object>> results =
                    redisTemplate.opsForZSet().reverseRangeWithScores(KEY_TRENDING_LUGARES, 0, limit - 1);

            if (results == null || results.isEmpty()) return Collections.emptyList();

            List<Map<String, Object>> lista = new ArrayList<>();
            for (ZSetOperations.TypedTuple<Object> tuple : results) {
                Map<String, Object> item = new HashMap<>();
                item.put("lugarId", tuple.getValue());
                item.put("puntosTrending", tuple.getScore());
                lista.add(item);
            }
            return lista;
        } catch (Exception ex) {
            log.warn("Error obteniendo trending desde Redis: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Rate limiter para peticiones al chatbot Gemini: máximo 20 peticiones por minuto por IP.
     */
    public boolean verificarLimiteRateChatbot(String ip) {
        if (redisTemplate == null || ip == null) return true; // Si no hay Redis, permitir por defecto
        try {
            String key = KEY_RATE_LIMIT_PREFIX + ip;
            Long peticiones = redisTemplate.opsForValue().increment(key);
            if (peticiones != null && peticiones == 1) {
                redisTemplate.expire(key, Duration.ofMinutes(1));
            }
            return peticiones != null && peticiones <= 20;
        } catch (Exception ex) {
            log.warn("Error en rate limiter Redis: {}", ex.getMessage());
            return true;
        }
    }
}
