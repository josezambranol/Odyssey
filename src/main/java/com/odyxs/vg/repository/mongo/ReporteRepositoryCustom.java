package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.Reporte;

import java.util.List;
import java.util.Map;

public interface ReporteRepositoryCustom {

    Map<String, Long> contarAgrupadoPorTipo();

    Map<String, Long> contarAgrupadoPorEstado();

    List<Map<String, Object>> obtenerTopEntidadesConMasReportes(int limit);

    void insertarLote(List<Reporte> reportes);
}
