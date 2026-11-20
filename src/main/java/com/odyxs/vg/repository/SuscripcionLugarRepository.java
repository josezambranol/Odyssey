package com.odyxs.vg.repository;

import com.odyxs.vg.entity.SuscripcionLugar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SuscripcionLugarRepository extends JpaRepository<SuscripcionLugar, String> {

    List<SuscripcionLugar> findByUsuarioIdAndActivoTrueOrderByFechaSuscripcionDesc(Long usuarioId);

    List<SuscripcionLugar> findByCorreoIgnoreCaseAndActivoTrueOrderByFechaSuscripcionDesc(String correo);

    Optional<SuscripcionLugar> findByUsuarioIdAndLugarId(Long usuarioId, Long lugarId);

    boolean existsByUsuarioIdAndLugarIdAndActivoTrue(Long usuarioId, Long lugarId);

    long countByLugarIdAndActivoTrue(Long lugarId);

    void deleteByLugarId(Long lugarId);
}
