package com.odyxs.vg.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.odyxs.vg.entity.Lugar;

public interface LugarRepository extends JpaRepository<Lugar, Long> {
    List<Lugar> findByCategoriaId(Long categoriaId);
    List<Lugar> findByEstado(Lugar.Estado estado);
    List<Lugar> findByCategoriaIdAndEstado(Long categoriaId, Lugar.Estado estado);
    List<Lugar> findByNombreContainingIgnoreCaseAndEstado(String texto, Lugar.Estado estado);

    @Query("SELECT l FROM Lugar l WHERE l.estado = :estado AND (" +
           "LOWER(l.nombre) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(COALESCE(l.descripcion, '')) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(COALESCE(l.ubicacion, '')) LIKE LOWER(CONCAT('%', :texto, '%')))")
    List<Lugar> buscarPorTexto(@Param("texto") String texto, @Param("estado") Lugar.Estado estado);

    List<Lugar> findByEstadoOrderByVisitasDesc(Lugar.Estado estado);

    boolean existsByCategoriaId(Long categoriaId);
    boolean existsByNombreAndDescripcionAndUbicacionAndUrlMapa(
        String nombre, String descripcion, String ubicacion, String urlMapa);
}