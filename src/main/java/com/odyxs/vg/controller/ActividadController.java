package com.odyxs.vg.controller;

import com.odyxs.vg.entity.Actividad;
import com.odyxs.vg.service.ActividadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class ActividadController {

    @Autowired private ActividadService actividadService;

    // ── Listado público (solo aprobadas) ──────────────────────
    @GetMapping("/actividades")
    public String actividades(@RequestParam(required = false) String cat, Model model) {
        List<Actividad> lista;
        if (cat != null && !cat.isBlank()) {
            try {
                lista = actividadService.obtenerPorCategoria(
                    Actividad.CategoriaActividad.valueOf(cat.toUpperCase()));
            } catch (IllegalArgumentException e) {
                lista = actividadService.obtenerAprobadas();
            }
        } else {
            lista = actividadService.obtenerAprobadas();
        }
        model.addAttribute("actividades", lista);
        model.addAttribute("catActiva", cat);
        return "actividades";
    }

    // ── Proponer actividad (usuarios autenticados) ────────────
    @GetMapping("/proponer-actividad")
    public String proponerForm() {
        return "proponer-actividad";
    }

    @PostMapping("/proponer-actividad")
    public String proponerActividad(@RequestParam String nombre,
                                    @RequestParam(required = false) String descripcion,
                                    @RequestParam(required = false) String duracion,
                                    @RequestParam(required = false) String precioAprox,
                                    @RequestParam String categoria,
                                    @RequestParam(required = false) MultipartFile imagen,
                                    Model model) {
        String resultado = actividadService.guardar(nombre, descripcion, duracion,
                                                    precioAprox, categoria, false, imagen);
        if ("Actividad guardada.".equals(resultado)) {
            model.addAttribute("mensaje", "¡Propuesta enviada! Será revisada por el equipo Odyssey.");
        } else {
            model.addAttribute("error", resultado);
        }
        return "proponer-actividad";
    }
}