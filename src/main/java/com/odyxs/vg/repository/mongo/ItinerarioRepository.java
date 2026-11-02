package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.Itinerario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItinerarioRepository extends MongoRepository<Itinerario, String> {

    Optional<Itinerario> findByCodigo(String codigo);

    List<Itinerario> findByUsuarioId(Long usuarioId);

    Page<Itinerario> findByEstiloViaje(String estiloViaje, Pageable pageable);
}
