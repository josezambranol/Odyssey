package com.odyxs.vg.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.repository.CategoriaRepository;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    public List<Categoria> obtenerTodas() {
        return categoriaRepository.findAll();
    }

    public Categoria obtenerPorId(Long id) {
        return categoriaRepository.findById(id).orElse(null);
    }

    public String guardar(String nombre) {
        if (categoriaRepository.existsByNombre(nombre)) {
            return "Ya existe esa categoría.";
        }
        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoriaRepository.save(categoria);
        return "Categoría guardada.";
    }
}