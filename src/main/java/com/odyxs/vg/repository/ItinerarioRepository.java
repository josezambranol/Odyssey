package com.odyxs.vg.repository;

import com.odyxs.vg.entity.Itinerario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItinerarioRepository extends JpaRepository<Itinerario, String> {

    Optional<Itinerario> findByCodigo(String codigo);

    List<Itinerario> findByUsuarioId(Long usuarioId);

    Page<Itinerario> findByEstiloViaje(String estiloViaje, Pageable pageable);

    void deleteByUsuarioId(Long usuarioId);
}
