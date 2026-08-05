package com.odyxs.vg.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.odyxs.vg.entity.Lugar;

public interface LugarRepository extends JpaRepository<Lugar, Long> {
    List<Lugar> findByCategoriaId(Long categoriaId);
    List<Lugar> findByEstado(Lugar.Estado estado);
    List<Lugar> findByCategoriaIdAndEstado(Long categoriaId, Lugar.Estado estado);
    List<Lugar> findByNombreContainingIgnoreCaseAndEstado(String texto, Lugar.Estado estado);

    boolean existsByCategoriaId(Long categoriaId);
    boolean existsByNombreAndDescripcionAndUbicacionAndUrlMapa(
        String nombre, String descripcion, String ubicacion, String urlMapa);
}