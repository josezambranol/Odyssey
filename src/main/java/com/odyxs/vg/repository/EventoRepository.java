package com.odyxs.vg.repository;
import com.odyxs.vg.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface EventoRepository extends JpaRepository<Evento, Long> {
    List<Evento> findByActivoTrueOrderByFechaAsc();
    List<Evento> findByActivoTrueAndFechaGreaterThanEqualOrderByFechaAsc(LocalDate fecha);
    List<Evento> findByActivoFalseOrderByIdDesc();
}
