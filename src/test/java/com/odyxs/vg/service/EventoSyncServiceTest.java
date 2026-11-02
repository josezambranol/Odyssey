package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.EventoScrapingLog;
import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.repository.mongo.EventoScrapingLogRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventoSyncServiceTest {

    @Mock
    private EventoRepository eventoRepository;

    @Mock
    private EventoScrapingLogRepository scrapingLogRepository;

    @Mock
    private ExternalEventCrawlerService crawlerService;

    @InjectMocks
    private EventoSyncService eventoSyncService;

    @Test
    void sincronizarEventosExternos_DeduplicaYPersiste() {
        Evento e1 = new Evento();
        e1.setNombre("Concierto en la Muralla");
        e1.setFecha(LocalDate.now().plusDays(3));
        e1.setLugar("Baluarte de Santiago");

        Evento e2 = new Evento();
        e2.setNombre("Feria Artesanal Getsemaní");
        e2.setFecha(LocalDate.now().plusDays(5));
        e2.setLugar("Plaza de la Trinidad");

        when(crawlerService.rastrearEventosExternos()).thenReturn(List.of(e1, e2));
        // e1 ya existe, e2 es nuevo
        when(eventoRepository.existsByNombreIgnoreCaseAndFecha(eq(e1.getNombre()), eq(e1.getFecha()))).thenReturn(true);
        when(eventoRepository.existsByNombreIgnoreCaseAndFecha(eq(e2.getNombre()), eq(e2.getFecha()))).thenReturn(false);

        Map<String, Object> res = eventoSyncService.sincronizarEventosExternos();

        assertNotNull(res);
        assertEquals("SUCCESS", res.get("status"));
        assertEquals(2, res.get("eventosRastreados"));
        assertEquals(1, res.get("nuevosEventosAgregados"));
        assertEquals(1, res.get("eventosDuplicadosOmitidos"));

        // Se verifica que solo se guarda e2 (el nuevo)
        verify(eventoRepository, times(1)).save(e2);
        verify(eventoRepository, never()).save(e1);
        verify(scrapingLogRepository, times(1)).save(any(EventoScrapingLog.class));
    }

    @Test
    void externalCrawler_ScrapingDesdeHtmlConJsoup() {
        ExternalEventCrawlerService crawler = new ExternalEventCrawlerService();

        String html = """
            <html>
                <body>
                    <div class="agenda-cultural">
                        <article class="evento-card">
                            <h3 class="title">Festival de Cumbia en La Tenaza</h3>
                            <p class="description">Presentación de grupos folclóricos de Bolívar.</p>
                            <span class="fecha">2026-11-15</span>
                            <span class="location">Monumento a La Tenaza, Cartagena</span>
                            <img src="https://cartagena.travel/img/cumbia.jpg" />
                        </article>
                    </div>
                </body>
            </html>
            """;

        List<Evento> eventos = crawler.scrapearDesdeHtml(html, "https://cartagena.travel/agenda");

        assertNotNull(eventos);
        assertEquals(1, eventos.size());
        Evento extraido = eventos.get(0);
        assertEquals("Festival de Cumbia en La Tenaza", extraido.getNombre());
        assertEquals("Presentación de grupos folclóricos de Bolívar.", extraido.getDescripcion());
        assertEquals(LocalDate.parse("2026-11-15"), extraido.getFecha());
        assertEquals("Monumento a La Tenaza, Cartagena", extraido.getLugar());
    }
}
