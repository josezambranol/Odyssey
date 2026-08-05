package com.odyxs.vg.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.odyxs.vg.entity.Categoria;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
    boolean existsByNombre(String nombre);
}
