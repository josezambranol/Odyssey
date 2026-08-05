package com.odyxs.vg.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.service.CategoriaService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @GetMapping("/categorias")
    public String listar(Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        List<Categoria> categorias = categoriaService.obtenerTodas();
        model.addAttribute("categorias", categorias);
        return "categorias";
    }

    @PostMapping("/categorias")
    public String guardar(@RequestParam String nombre,
                        HttpSession session) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        categoriaService.guardar(nombre);
        return "redirect:/categorias";
    }
}