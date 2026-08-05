package com.odyxs.vg.repository;

import com.odyxs.vg.entity.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {
    List<Actividad> findByCategoria(Actividad.CategoriaActividad categoria);
    List<Actividad> findByEstado(Actividad.Estado estado);
    List<Actividad> findByCategoriaAndEstado(Actividad.CategoriaActividad categoria, Actividad.Estado estado);
}
