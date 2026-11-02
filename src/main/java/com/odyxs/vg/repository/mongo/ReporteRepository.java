package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReporteRepository extends MongoRepository<Reporte, String>, ReporteRepositoryCustom {

    Optional<Reporte> findByCodigo(String codigo);

    Page<Reporte> findByTipo(Reporte.TipoReporte tipo, Pageable pageable);

    Page<Reporte> findByEstado(Reporte.EstadoReporte estado, Pageable pageable);

    Page<Reporte> findByTipoAndEstado(Reporte.TipoReporte tipo, Reporte.EstadoReporte estado, Pageable pageable);

    Page<Reporte> findByPrioridad(Reporte.Prioridad prioridad, Pageable pageable);

    Page<Reporte> findByEntidadId(Long entidadId, Pageable pageable);

    Page<Reporte> findAllBy(TextCriteria criteria, Pageable pageable);

    long countByTipo(Reporte.TipoReporte tipo);

    long countByEstado(Reporte.EstadoReporte estado);
}
