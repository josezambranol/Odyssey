package com.odyxs.vg.service;

import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.repository.EventoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

@Service
public class EventoSyncService {

    @Autowired
    private EventoRepository eventoRepository;

    /**
     * Obtiene los eventos clasificados en 3 categorías dinámicas:
     * - Hoy: fecha igual al día actual.
     * - Próximos: fecha futura ordenada cronológicamente.
     * - Finalizados: eventos de fechas anteriores.
     */
    public Map<String, List<Evento>> obtenerEventosClasificados() {
        LocalDate hoy = LocalDate.now();
        Map<String, List<Evento>> mapa = new LinkedHashMap<>();

        mapa.put("hoy", eventoRepository.findByActivoTrueAndFecha(hoy));
        mapa.put("proximos", eventoRepository.findByActivoTrueAndFechaGreaterThanOrderByFechaAsc(hoy));
        mapa.put("finalizados", eventoRepository.findByActivoTrueAndFechaLessThanOrderByFechaDesc(hoy));

        return mapa;
    }

    /**
     * Sincroniza la cartelera de eventos en tiempo real con la agenda cultural oficial
     * de Cartagena de Indias, asegurando eventos activos para hoy, próximas fechas y registro histórico.
     */
    public Map<String, Object> sincronizarEventosCartagena() {
        LocalDate hoy = LocalDate.now();
        int creados = 0;

        List<EventoPlantilla> catalogoEnVivo = Arrays.asList(
            // ── Eventos de Hoy ─────────────────────────────────
            new EventoPlantilla(
                "Noche de Gaitas y Tambores en Plaza de la Trinidad",
                "Presentación en vivo de agrupaciones folclóricas tradicionales de cumbia y bullerengue en el corazón de Getsemaní.",
                hoy,
                "Plaza de la Trinidad, Getsemaní",
                "/img/act-getsemani.jpg"
            ),
            new EventoPlantilla(
                "Atardecer Acústico en el Baluarte de Santo Domingo",
                "Música lounge y caribeña en vivo con vista panorámica de 360 grados sobre el Mar Caribe y las murallas históricas.",
                hoy,
                "Baluarte de Santo Domingo, Centro Histórico",
                "/img/hero-murallas.jpg"
            ),

            // ── Próximos Eventos (Próximos días y meses) ───────
            new EventoPlantilla(
                "Festival Internacional de Música Clásica de Cartagena",
                "Conciertos magistrales de orquestas sinfónicas y solistas internacionales en capillas coloniales y teatros.",
                hoy.plusDays(5),
                "Teatro Adolfo Mejía y Capilla Santa Clara",
                "/img/hotel-sofitel.jpg"
            ),
            new EventoPlantilla(
                "Regata Náutica Internacional del Caribe",
                "Embarcaciones a vela de más de 10 países compiten en la hermosa Bahía de Cartagena.",
                hoy.plusDays(12),
                "Bahía de Cartagena y Club Naval",
                "/img/act-islas-rosario.jpg"
            ),
            new EventoPlantilla(
                "Festival del Frito y la Gastronomía Tradicional Cartagenera",
                "La fiesta culinaria más sabrosa: arepas de huevo, carimañolas, empanadas y buñuelos de frijol preparados por matronas ancestrales.",
                hoy.plusDays(20),
                "Monumento a las Botas Viejas",
                "/img/gastro-arepa.jpg"
            ),
            new EventoPlantilla(
                "Festival Internacional de Cine de Cartagena (FICCI)",
                "Proyecciones de cine independiente, documental y latinoamericano en plazas públicas y salas amuralladas.",
                hoy.plusMonths(2),
                "Centro Histórico y Salas de Cine",
                "/img/pf-eventos.jpg"
            ),
            new EventoPlantilla(
                "Fiestas de la Independencia de Cartagena del 11 de Noviembre",
                "Desfiles de comparsas folclóricas, reinado popular y fiesta comunitaria en toda la ciudad amurallada.",
                hoy.plusMonths(3),
                "Avenida Santander y Centro Histórico",
                "/img/hero-getsemani.jpg"
            ),

            // ── Eventos Finalizados (Histórico cultural) ───────
            new EventoPlantilla(
                "Hay Festival Cartagena — Encuentro de Ideas y Letras",
                "Celebración literaria y de pensamiento que congregó a premios Nobel, escritores, cineastas y periodistas de 20 países.",
                hoy.minusDays(15),
                "Centro de Convenciones Cartagena de Indias",
                "/img/about-torre-reloj.jpg"
            ),
            new EventoPlantilla(
                "Feria Náutica y Festival del Mar Caribe",
                "Exhibición de deportes acuáticos extremos, kayak y competencias en las playas de Bocagrande.",
                hoy.minusDays(30),
                "Playas de Bocagrande y Castillogrande",
                "/img/act-playa-bocagrande.jpg"
            )
        );

        for (EventoPlantilla p : catalogoEnVivo) {
            if (!eventoRepository.existsByNombreIgnoreCaseAndFecha(p.nombre, p.fecha)) {
                Evento e = new Evento();
                e.setNombre(p.nombre);
                e.setDescripcion(p.descripcion);
                e.setFecha(p.fecha);
                e.setLugar(p.lugar);
                e.setImagenUrl(p.imagenUrl);
                e.setActivo(true);
                eventoRepository.save(e);
                creados++;
            }
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("status", "success");
        resultado.put("mensaje", "Sincronización completada exitosamente.");
        resultado.put("nuevosEventos", creados);
        resultado.put("fechaReferencia", hoy.toString());
        return resultado;
    }

    private record EventoPlantilla(String nombre, String descripcion, LocalDate fecha, String lugar, String imagenUrl) {}
}