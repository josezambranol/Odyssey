package com.odyxs.vg.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.odyxs.vg.entity.Resenas;

public interface ResenaRepository extends JpaRepository<Resenas, Long> {
    List<Resenas> findByLugarId(Long lugarId);
}