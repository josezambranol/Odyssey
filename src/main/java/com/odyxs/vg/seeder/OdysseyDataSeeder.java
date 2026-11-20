package com.odyxs.vg.seeder;

import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.entity.Reporte;
import com.odyxs.vg.repository.CategoriaRepository;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.repository.ReporteRepository;
import com.odyxs.vg.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class OdysseyDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OdysseyDataSeeder.class);

    private static final Map<String, double[]> COORDENADAS_BASE = Map.ofEntries(
            Map.entry("Playa Blanca – Isla de Barú", new double[]{10.2281, -75.6148}),
            Map.entry("Playa de Bocagrande", new double[]{10.4022, -75.5567}),
            Map.entry("Museo del Oro Zenú", new double[]{10.4235, -75.5508}),
            Map.entry("Museo Naval del Caribe", new double[]{10.4228, -75.5515}),
            Map.entry("Cine Colombia Bocagrande", new double[]{10.4041, -75.5542}),
            Map.entry("Hotel Almirante Cartagena", new double[]{10.3992, -75.5582}),
            Map.entry("Charleston Santa Teresa", new double[]{10.4229, -75.5516}),
            Map.entry("Murallas de Cartagena", new double[]{10.4241, -75.5518}),
            Map.entry("Torre del Reloj", new double[]{10.4233, -75.5489}),
            Map.entry("Castillo de San Felipe de Barajas", new double[]{10.4225, -75.5398}),
            Map.entry("La Cevichería", new double[]{10.4262, -75.5458}),
            Map.entry("Sofitel Legend Santa Clara", new double[]{10.4278, -75.5463}),
            Map.entry("Bastión Luxury Hotel", new double[]{10.4248, -75.5492}),
            Map.entry("Centro de Convenciones Cartagena de Indias", new double[]{10.4216, -75.5468})
    );

    @Autowired(required = false)
    private ReporteRepository reporteRepository;

    @Autowired(required = false)
    private LugarRepository lugarRepository;

    @Autowired(required = false)
    private CategoriaRepository categoriaRepository;

    @Autowired(required = false)
    private UsuarioService usuarioService;

    @Override
    public void run(String... args) {
        if (usuarioService != null) {
            try {
                usuarioService.inicializarAdmin();
            } catch (Exception ex) {
                log.warn("[ODYSSEY] No se pudo inicializar admin en MySQL: {}", ex.getMessage());
            }
        }

        if (lugarRepository != null) {
            try {
                List<Lugar> lugares = lugarRepository.findAll();
                int actualizados = 0;
                for (Lugar l : lugares) {
                    if ((l.getLatitud() == null || l.getLongitud() == null) && l.getNombre() != null) {
                        double[] coords = COORDENADAS_BASE.get(l.getNombre());
                        if (coords != null) {
                            l.setLatitud(coords[0]);
                            l.setLongitud(coords[1]);
                            lugarRepository.save(l);
                            actualizados++;
                        }
                    }
                }
                if (actualizados > 0) {
                    log.info("[ODYSSEY] Coordenadas geográficas sincronizadas en MySQL para {} lugares.", actualizados);
                }

                // Garantizar que la categoría FESTIVALES tenga al menos una sede representativa
                if (categoriaRepository != null && !lugares.isEmpty()) {
                    boolean tieneFestival = lugares.stream()
                            .anyMatch(l -> l.getCategoria() != null && "FESTIVALES".equalsIgnoreCase(l.getCategoria().getNombre()));
                    if (!tieneFestival) {
                        categoriaRepository.findAll().stream()
                                .filter(c -> "FESTIVALES".equalsIgnoreCase(c.getNombre()))
                                .findFirst()
                                .ifPresent(catFest -> {
                                    Lugar sede = new Lugar();
                                    sede.setNombre("Centro de Convenciones Cartagena de Indias");
                                    sede.setDescripcion("Sede emblemática frente a la Bahía de las Ánimas para el FICCI, el Hay Festival y el Festival Internacional de Música.");
                                    sede.setUbicacion("Calle 24 #8A-344, Getsemaní, Cartagena");
                                    sede.setUrlMapa("https://maps.app.goo.gl/ccci");
                                    sede.setImagenUrl("/img/pf-eventos.jpg");
                                    sede.setCategoria(catFest);
                                    sede.setLatitud(10.4216);
                                    sede.setLongitud(-75.5468);
                                    sede.setEsOficial(true);
                                    sede.setEstado(Lugar.Estado.APROBADO);
                                    lugarRepository.save(sede);
                                    log.info("[ODYSSEY] Sede representativa añadida a la categoría FESTIVALES.");
                                });
                    }
                }
            } catch (Exception ex) {
                log.warn("[ODYSSEY] No se pudieron sincronizar coordenadas en MySQL: {}", ex.getMessage());
            }
        }

        if (reporteRepository != null) {
            try {
                long total = reporteRepository.count();
                if (total == 0) {
                    Reporte r1 = new Reporte();
                    r1.setCodigo("REP-INIT-001");
                    r1.setTipo(Reporte.TipoReporte.VISITA_TURISTICA);
                    r1.setTitulo("Bienvenida a la Guía Inteligente Odyssey");
                    r1.setDescripcion("Telemetría inicial del sistema para Cartagena de Indias.");
                    r1.setNombreEntidad("Torre del Reloj");
                    r1.setEntidadId(9L);
                    r1.setPrioridad(Reporte.Prioridad.BAJA);
                    r1.setEstado(Reporte.EstadoReporte.RESUELTO);
                    r1.setFechaCreacion(Instant.now());
                    r1.setMetadata(Map.of("sistema", "Odyssey MySQL", "modo", "Monolito"));

                    Reporte r2 = new Reporte();
                    r2.setCodigo("REP-INIT-002");
                    r2.setTipo(Reporte.TipoReporte.CONSULTA_IA);
                    r2.setTitulo("Interacción con Asistente Virtual Gemini");
                    r2.setDescripcion("Consulta turística sobre playas y gastronomía en Cartagena.");
                    r2.setNombreEntidad("Playa Blanca – Isla de Barú");
                    r2.setEntidadId(1L);
                    r2.setPrioridad(Reporte.Prioridad.MEDIA);
                    r2.setEstado(Reporte.EstadoReporte.RESUELTO);
                    r2.setFechaCreacion(Instant.now());

                    reporteRepository.saveAll(List.of(r1, r2));
                    log.info("[ODYSSEY] Reportes iniciales cargados en MySQL.");
                }
            } catch (Exception ex) {
                log.warn("[ODYSSEY] No se pudieron inicializar reportes en MySQL: {}", ex.getMessage());
            }
        }
    }
}
