package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.EventoScrapingLog;
import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.repository.mongo.EventoScrapingLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class EventoSyncService {

    private static final Logger log = LoggerFactory.getLogger(EventoSyncService.class);

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired(required = false)
    private EventoScrapingLogRepository scrapingLogRepository;

    @Autowired
    private ExternalEventCrawlerService crawlerService;

    /**
     * Obtiene los eventos clasificados con soporte de caché en Redis (15 min).
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
     * 🌐 Sincronización Avanzada con Web Scraping e Ingesta Externa de Eventos Culturales.
     * Rastrea portales y agendas externas, deduplica por nombre y fecha,
     * persiste los eventos nuevos en MySQL y guarda la auditoría de scraping en MongoDB.
     */
    @CacheEvict(value = "eventosClasificados", allEntries = true)
    public Map<String, Object> sincronizarEventosExternos() {
        long inicio = System.currentTimeMillis();
        log.info("[EVENTO_SYNC] Iniciando rastreo y sincronización de eventos externos de Cartagena...");

        List<Evento> recolectados = crawlerService.rastrearEventosExternos();
        int nuevos = 0;
        int duplicados = 0;
        List<Map<String, String>> muestra = new ArrayList<>();

        for (Evento e : recolectados) {
            boolean yaExiste = eventoRepository.existsByNombreIgnoreCaseAndFecha(e.getNombre(), e.getFecha());
            if (!yaExiste) {
                eventoRepository.save(e);
                nuevos++;
                if (muestra.size() < 5) {
                    Map<String, String> item = new HashMap<>();
                    item.put("nombre", e.getNombre() != null ? e.getNombre() : "");
                    item.put("fecha", e.getFecha() != null ? e.getFecha().toString() : "");
                    item.put("lugar", e.getLugar() != null ? e.getLugar() : "");
                    muestra.add(item);
                }
            } else {
                duplicados++;
            }
        }

        long duracionMs = System.currentTimeMillis() - inicio;

        // Registro de telemetría y auditoría de scraping en MongoDB
        guardarLogScrapingEnMongo("https://cartagena.gov.co/cultura-y-turismo", "Cartagena Cultural Agendas & Web Feeds",
                recolectados.size(), nuevos, duplicados, duracionMs, muestra);

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("status", "SUCCESS");
        resultado.put("mensaje", "Sincronización externa de eventos completada con éxito.");
        resultado.put("eventosRastreados", recolectados.size());
        resultado.put("nuevosEventosAgregados", nuevos);
        resultado.put("eventosDuplicadosOmitidos", duplicados);
        resultado.put("tiempoProcesamientoMs", duracionMs);
        resultado.put("fechaSincronizacion", LocalDate.now().toString());

        log.info("[EVENTO_SYNC] Finalizado con éxito en {} ms. Nuevos: {}, Duplicados: {}", duracionMs, nuevos, duplicados);
        return resultado;
    }

    /**
     * Tarea programada: Sincroniza automáticamente la cartelera todos los días a las 5:00 AM.
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void tareaProgramadaSincronizacion() {
        log.info("[CRON] Ejecutando sincronización automática diaria de eventos de Cartagena...");
        sincronizarEventosExternos();
    }

    private void guardarLogScrapingEnMongo(String url, String fuente, int total, int nuevos, int duplicados,
                                           long latenciaMs, List<Map<String, String>> muestra) {
        if (scrapingLogRepository == null) return;
        try {
            EventoScrapingLog logDoc = new EventoScrapingLog();
            logDoc.setFuenteUrl(url);
            logDoc.setNombreFuente(fuente);
            logDoc.setEventosExtraidos(total);
            logDoc.setEventosNuevos(nuevos);
            logDoc.setEventosDuplicados(duplicados);
            logDoc.setLatenciaMs(latenciaMs);
            logDoc.setEventosMuestra(muestra);
            logDoc.setExito(true);
            scrapingLogRepository.save(logDoc);
        } catch (Exception ex) {
            log.warn("[EVENTO_SYNC] No se pudo guardar el log de scraping en MongoDB: {}", ex.getMessage());
        }
    }
}
