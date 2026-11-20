package com.odyxs.vg.controller;

import com.odyxs.vg.service.CategoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @PostMapping("/categorias")
    public String guardar(@RequestParam String nombre) {
        if (nombre != null && !nombre.isBlank()) {
            categoriaService.guardar(nombre.trim());
        }
        return "redirect:/admin";
    }
}
