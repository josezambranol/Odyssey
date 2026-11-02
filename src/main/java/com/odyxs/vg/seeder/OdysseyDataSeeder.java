package com.odyxs.vg.seeder;

import com.odyxs.vg.domain.mongo.Reporte;
import com.odyxs.vg.repository.mongo.ReporteRepository;
import com.odyxs.vg.service.UsuarioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class OdysseyDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OdysseyDataSeeder.class);

    @Autowired(required = false)
    private ReporteRepository reporteRepository;

    @Autowired(required = false)
    private UsuarioService usuarioService;

    @Override
    public void run(String... args) {
        // Inicializar administrador por defecto en MySQL
        if (usuarioService != null) {
            try {
                usuarioService.inicializarAdmin();
            } catch (Exception ex) {
                log.warn("[ODYSSEY] No se pudo inicializar admin en MySQL (DB detenida o no accesible): {}", ex.getMessage());
            }
        }

        // Inicializar datos semilla en MongoDB si está accesible
        if (reporteRepository != null) {
            try {
                long total = reporteRepository.count();
                log.info("[ODYSSEY] Estado NoSQL: {} reportes registrados en MongoDB.", total);

                if (total == 0) {
                    log.info("[ODYSSEY] Inicializando datos semilla de bienvenida para MongoDB...");
                    List<Reporte> iniciales = new ArrayList<>();

                    Reporte r1 = new Reporte();
                    r1.setCodigo("REP-INIT-001");
                    r1.setTipo(Reporte.TipoReporte.VISITA_TURISTICA);
                    r1.setTitulo("Bienvenida a la Guía Inteligente Odyssey");
                    r1.setDescripcion("Telemetría inicial del sistema para Cartagena de Indias.");
                    r1.setNombreEntidad("Torre del Reloj");
                    r1.setEntidadId(1L);
                    r1.setPrioridad(Reporte.Prioridad.BAJA);
                    r1.setEstado(Reporte.EstadoReporte.RESUELTO);
                    r1.setFechaCreacion(Instant.now());
                    r1.setMetadata(Map.of("sistema", "Odyssey Polyglot 2.0", "modo", "Enterprise"));
                    iniciales.add(r1);

                    Reporte r2 = new Reporte();
                    r2.setCodigo("REP-INIT-002");
                    r2.setTipo(Reporte.TipoReporte.CONSULTA_IA);
                    r2.setTitulo("Interacción con Asistente Virtual Gemini");
                    r2.setDescripcion("Consulta turística sobre mejores playas y restaurantes en Getsemaní.");
                    r2.setNombreEntidad("Barrio Getsemaní");
                    r2.setEntidadId(2L);
                    r2.setPrioridad(Reporte.Prioridad.MEDIA);
                    r2.setEstado(Reporte.EstadoReporte.RESUELTO);
                    r2.setFechaCreacion(Instant.now());
                    iniciales.add(r2);

                    reporteRepository.insertarLote(iniciales);
                    log.info("[ODYSSEY] Semilla inicial cargada en MongoDB.");
                }

                log.info("[ODYSSEY] 🚀 Nota: Para ejecutar la prueba de estrés de +15,000 documentos, llame al endpoint POST /api/v1/reportes/benchmark/seed-15k");
            } catch (Exception ex) {
                log.warn("[ODYSSEY] MongoDB no está accesible en este arranque (se ejecutará en modo degradado si no está iniciado el servicio): {}", ex.getMessage());
            }
        }
    }
}
