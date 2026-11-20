package com.odyxs.vg.repository;

import com.odyxs.vg.entity.CodigoVerificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, String> {

    Optional<CodigoVerificacion> findFirstByCorreoIgnoreCaseAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(
            String correo, String codigo);

    Optional<CodigoVerificacion> findFirstByCorreoIgnoreCaseAndTipoAndUsadoFalseOrderByFechaCreacionDesc(
            String correo, CodigoVerificacion.TipoCodigo tipo);

    long deleteByExpiraEnBefore(Instant fechaLimite);
}
