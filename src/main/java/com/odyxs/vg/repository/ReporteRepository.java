package com.odyxs.vg.repository;

import com.odyxs.vg.entity.Reporte;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, String> {

    Optional<Reporte> findByCodigo(String codigo);

    Page<Reporte> findByTipo(Reporte.TipoReporte tipo, Pageable pageable);

    Page<Reporte> findByEstado(Reporte.EstadoReporte estado, Pageable pageable);

    Page<Reporte> findByTipoAndEstado(Reporte.TipoReporte tipo, Reporte.EstadoReporte estado, Pageable pageable);

    Page<Reporte> findByPrioridad(Reporte.Prioridad prioridad, Pageable pageable);

    Page<Reporte> findByEntidadId(Long entidadId, Pageable pageable);

    @Query("SELECT r FROM Reporte r WHERE LOWER(r.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(r.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) " +
           "OR LOWER(r.nombreEntidad) LIKE LOWER(CONCAT('%', :texto, '%'))")
    Page<Reporte> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("SELECT r.tipo, COUNT(r) FROM Reporte r WHERE r.tipo IS NOT NULL GROUP BY r.tipo ORDER BY COUNT(r) DESC")
    List<Object[]> contarAgrupadoPorTipoRaw();

    @Query("SELECT r.estado, COUNT(r) FROM Reporte r WHERE r.estado IS NOT NULL GROUP BY r.estado ORDER BY COUNT(r) DESC")
    List<Object[]> contarAgrupadoPorEstadoRaw();

    @Query("SELECT r.entidadId, r.nombreEntidad, COUNT(r) FROM Reporte r WHERE r.entidadId IS NOT NULL " +
           "GROUP BY r.entidadId, r.nombreEntidad ORDER BY COUNT(r) DESC")
    List<Object[]> obtenerTopEntidadesRaw(Pageable pageable);

    long countByTipo(Reporte.TipoReporte tipo);

    long countByEstado(Reporte.EstadoReporte estado);
}
