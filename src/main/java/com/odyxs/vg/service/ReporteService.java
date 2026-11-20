package com.odyxs.vg.service;

import com.odyxs.vg.entity.Reporte;
import com.odyxs.vg.repository.ReporteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    @Autowired
    private ReporteRepository reporteRepository;

    /**
     * Exportación masiva paginada en CSV desde MySQL con consumo mínimo de memoria.
     */
    @Transactional(readOnly = true)
    public void exportarReportesCsvStreaming(OutputStream os, Reporte.TipoReporte tipo) throws IOException {
        os.write(0xEF);
        os.write(0xBB);
        os.write(0xBF);

        PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8));
        writer.println("ID,Codigo,Tipo,Titulo,Entidad,Prioridad,Estado,UsuarioEmail,FechaCreacion");

        int page = 0;
        final int pageSize = 500;
        Page<Reporte> batch;
        do {
            Pageable pageable = PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "fechaCreacion"));
            batch = (tipo != null)
                    ? reporteRepository.findByTipo(tipo, pageable)
                    : reporteRepository.findAll(pageable);

            for (Reporte r : batch.getContent()) {
                String linea = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        r.getId() != null ? r.getId() : "",
                        r.getCodigo() != null ? r.getCodigo() : "",
                        r.getTipo() != null ? r.getTipo().name() : "",
                        r.getTitulo() != null ? r.getTitulo().replace("\"", "\"\"") : "",
                        r.getNombreEntidad() != null ? r.getNombreEntidad().replace("\"", "\"\"") : "",
                        r.getPrioridad() != null ? r.getPrioridad().name() : "",
                        r.getEstado() != null ? r.getEstado().name() : "",
                        r.getUsuarioEmail() != null ? r.getUsuarioEmail() : "",
                        r.getFechaCreacion() != null ? r.getFechaCreacion().toString() : ""
                );
                writer.println(linea);
            }
            page++;
        } while (batch.hasNext());

        writer.flush();
    }

    public Page<Reporte> listarPaginado(Reporte.TipoReporte tipo, Reporte.EstadoReporte estado, Pageable pageable) {
        if (tipo != null && estado != null) {
            return reporteRepository.findByTipoAndEstado(tipo, estado, pageable);
        } else if (tipo != null) {
            return reporteRepository.findByTipo(tipo, pageable);
        } else if (estado != null) {
            return reporteRepository.findByEstado(estado, pageable);
        }
        return reporteRepository.findAll(pageable);
    }

    public Page<Reporte> buscarTexto(String texto, Pageable pageable) {
        return reporteRepository.buscarPorTexto(texto, pageable);
    }

    public Optional<Reporte> buscarPorId(String id) {
        return reporteRepository.findById(id);
    }

    public Optional<Reporte> buscarPorCodigo(String codigo) {
        return reporteRepository.findByCodigo(codigo);
    }

    @CacheEvict(value = "reportesMetricas", allEntries = true)
    public Reporte guardar(Reporte reporte) {
        if (reporte.getCodigo() == null || reporte.getCodigo().isBlank()) {
            reporte.setCodigo("REP-" + System.currentTimeMillis() + "-" + (int) (Math.random() * 9000 + 1000));
        }
        return reporteRepository.save(reporte);
    }

    @Cacheable(value = "reportesMetricas", key = "'global'")
    public Map<String, Object> obtenerEstadisticasGlobales() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalReportes", reporteRepository.count());
        stats.put("conteoPorTipo", mapearConteo(reporteRepository.contarAgrupadoPorTipoRaw()));
        stats.put("conteoPorEstado", mapearConteo(reporteRepository.contarAgrupadoPorEstadoRaw()));
        stats.put("topLugaresReportados", mapearTopEntidades(reporteRepository.obtenerTopEntidadesRaw(PageRequest.of(0, 5))));
        stats.put("fechaCalculo", Instant.now().toString());
        return stats;
    }

    @CacheEvict(value = "reportesMetricas", allEntries = true)
    @Transactional
    public Map<String, Object> ejecutarBenchmark15000Documentos() {
        log.info("Iniciando Benchmark: Generando 15,000 reportes en MySQL...");
        long tiempoInicioTotal = System.currentTimeMillis();

        int totalGenerar = 15000;
        int tamanoLote = 1000;
        int lotes = totalGenerar / tamanoLote;

        String[] lugaresCartagena = {
                "Castillo de San Felipe", "Torre del Reloj", "Islas del Rosario",
                "Playa Blanca (Barú)", "Murallas de Cartagena", "Barrio Getsemaní",
                "Convento de la Popa", "Palacio de la Inquisición", "Playa Bocagrande"
        };

        Reporte.TipoReporte[] tipos = Reporte.TipoReporte.values();
        Reporte.Prioridad[] prioridades = Reporte.Prioridad.values();
        Reporte.EstadoReporte[] estados = Reporte.EstadoReporte.values();
        Random random = new Random();

        for (int l = 0; l < lotes; l++) {
            List<Reporte> listaLote = new ArrayList<>(tamanoLote);
            for (int i = 0; i < tamanoLote; i++) {
                int indexGlobal = (l * tamanoLote) + i + 1;
                Reporte rep = new Reporte();
                rep.setCodigo("BENCH-15K-" + System.currentTimeMillis() + "-" + String.format("%06d", indexGlobal));
                rep.setTipo(tipos[random.nextInt(tipos.length)]);
                rep.setPrioridad(prioridades[random.nextInt(prioridades.length)]);
                rep.setEstado(estados[random.nextInt(estados.length)]);

                int lugarIdx = random.nextInt(lugaresCartagena.length);
                rep.setEntidadId((long) (lugarIdx + 1));
                rep.setNombreEntidad(lugaresCartagena[lugarIdx]);
                rep.setTitulo("Reporte de actividad turística #" + indexGlobal + " en " + rep.getNombreEntidad());
                rep.setDescripcion("Registro automatizado para telemetría turística en Cartagena de Indias.");
                rep.setUsuarioEmail("turista_" + (random.nextInt(500) + 1) + "@odyssey.test");
                rep.setMetadata(Map.of(
                        "latenciaServicioMs", random.nextInt(350) + 15,
                        "zona", lugarIdx % 2 == 0 ? "Centro Histórico" : "Zona Insular"
                ));
                rep.setFechaCreacion(Instant.now().minus(random.nextInt(30), ChronoUnit.DAYS));
                if (rep.getEstado() == Reporte.EstadoReporte.RESUELTO) {
                    rep.setFechaResolucion(rep.getFechaCreacion().plus(random.nextInt(24), ChronoUnit.HOURS));
                }
                listaLote.add(rep);
            }
            reporteRepository.saveAll(listaLote);
        }

        long tiempoInsercionTotalMs = System.currentTimeMillis() - tiempoInicioTotal;
        long inicioLectura = System.nanoTime();
        Page<Reporte> paginaPrueba = reporteRepository.findByTipo(
                Reporte.TipoReporte.VISITA_TURISTICA,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fechaCreacion"))
        );
        long duracionLecturaMs = (System.nanoTime() - inicioLectura) / 1_000_000;

        Map<String, Object> benchmark = new LinkedHashMap<>();
        benchmark.put("status", "SUCCESS");
        benchmark.put("documentosGeneradosEnEstaPrueba", totalGenerar);
        benchmark.put("totalDocumentosEnColeccion", reporteRepository.count());
        benchmark.put("tiempoTotalInsercionMs", tiempoInsercionTotalMs);
        benchmark.put("latenciaConsultaPaginadaMs", duracionLecturaMs + " ms");
        benchmark.put("muestraPagina", "Total elementos tipo VISITA_TURISTICA: " + paginaPrueba.getTotalElements());
        return benchmark;
    }

    private Map<String, Long> mapearConteo(List<Object[]> rows) {
        Map<String, Long> map = new LinkedHashMap<>();
        if (rows == null) return map;
        for (Object[] row : rows) {
            if (row[0] != null && row[1] instanceof Number n) {
                map.put(row[0].toString(), n.longValue());
            }
        }
        return map;
    }

    private List<Map<String, Object>> mapearTopEntidades(List<Object[]> rows) {
        List<Map<String, Object>> lista = new ArrayList<>();
        if (rows == null) return lista;
        for (Object[] row : rows) {
            Map<String, Object> item = new HashMap<>();
            item.put("entidadId", row[0]);
            item.put("nombreEntidad", row[1]);
            item.put("totalReportes", row[2]);
            lista.add(item);
        }
        return lista;
    }
}
