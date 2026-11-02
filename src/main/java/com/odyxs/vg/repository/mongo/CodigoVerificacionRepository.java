package com.odyxs.vg.repository.mongo;

import com.odyxs.vg.domain.mongo.CodigoVerificacion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends MongoRepository<CodigoVerificacion, String> {

    Optional<CodigoVerificacion> findFirstByCorreoIgnoreCaseAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(
            String correo, String codigo);

    Optional<CodigoVerificacion> findFirstByCorreoIgnoreCaseAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
            String correo, CodigoVerificacion.TipoCodigo tipo);
}
