package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.SuscripcionLugar;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionLugarRepository extends MongoRepository<SuscripcionLugar, String> {

    List<SuscripcionLugar> findByUsuarioIdAndActivoTrueOrderByFechaSuscripcionDesc(Long usuarioId);

    List<SuscripcionLugar> findByCorreoIgnoreCaseAndActivoTrueOrderByFechaSuscripcionDesc(String correo);

    Optional<SuscripcionLugar> findByUsuarioIdAndLugarId(Long usuarioId, Long lugarId);

    boolean existsByUsuarioIdAndLugarIdAndActivoTrue(Long usuarioId, Long lugarId);

    long countByLugarIdAndActivoTrue(Long lugarId);
}
