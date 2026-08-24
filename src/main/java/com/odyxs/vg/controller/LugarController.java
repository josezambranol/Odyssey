package com.odyxs.vg.controller;

import com.odyxs.vg.entity.*;
import com.odyxs.vg.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class LugarController {

    @Autowired private LugarService      lugarService;
    @Autowired private ResenaService     resenaService;
    @Autowired private CategoriaService  categoriaService;

    // ── Listado por categoría (Público) ───────────────────────
    @GetMapping("/lugares")
    public String lugares(@RequestParam(required = false) Long categoriaId, Model model) {
        List<Lugar> lista = (categoriaId == null || categoriaId == 0)
                ? lugarService.obtenerAprobados()
                : lugarService.obtenerPorCategoriaAprobados(categoriaId);

        String catNombre = null;
        String catNombreClave = null;
        if (!lista.isEmpty() && categoriaId != null && categoriaId != 0) {
            catNombre = lista.get(0).getCategoria().getNombre();
            catNombreClave = lista.get(0).getCategoria().getNombreClave();
        } else if (categoriaId != null && categoriaId != 0) {
            var catOpt = categoriaService.obtenerTodas().stream()
                .filter(c -> c.getId().equals(categoriaId))
                .findFirst();
            if (catOpt.isPresent()) {
                catNombre = catOpt.get().getNombre();
                catNombreClave = catOpt.get().getNombreClave();
            }
        }

        model.addAttribute("lugares",       lista);
        model.addAttribute("categorias",    categoriaService.obtenerTodas());
        model.addAttribute("categoriaId",   categoriaId);
        model.addAttribute("catNombre",     catNombre);
        model.addAttribute("catNombreClave", catNombreClave);
        return "lugares";
    }

    // ── Detalle del lugar (Público) ───────────────────────────
    @GetMapping("/lugares/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Lugar lugar = lugarService.obtenerPorId(id);
        if (lugar == null) return "redirect:/lugares";
        model.addAttribute("lugar",   lugar);
        model.addAttribute("resenas", resenaService.obtenerPorLugar(id));
        return "detalle";
    }

    // ── Búsqueda global (Público) ─────────────────────────────
    @GetMapping("/buscar")
    public String buscar(@RequestParam(defaultValue = "") String q, Model model) {
        List<Lugar> resultados = q.isBlank()
                ? lugarService.obtenerAprobados()
                : lugarService.buscar(q);
        model.addAttribute("lugares", resultados);
        model.addAttribute("query",   q);
        return "buscar";
    }

    // ── Proponer lugar (Usuarios autenticados) ─────────────────
    @GetMapping("/proponer-lugar")
    public String proponerForm(Model model) {
        model.addAttribute("categorias", categoriaService.obtenerTodas());
        return "proponer-lugar";
    }

    @PostMapping("/proponer-lugar")
    public String proponerLugar(@RequestParam String nombre,
                                @RequestParam(required = false) String descripcion,
                                @RequestParam String ubicacion,
                                @RequestParam Long categoriaId,
                                @RequestParam(required = false) String urlMapa,
                                @RequestParam(required = false) MultipartFile imagen,
                                Model model) {
        String resultado = lugarService.guardar(categoriaId, nombre, descripcion, ubicacion, urlMapa, false, imagen);

        if ("Lugar guardado.".equals(resultado)) {
            model.addAttribute("mensaje", "¡Propuesta enviada! Será revisada por el equipo ODYXS.");
        } else {
            model.addAttribute("error", resultado);
        }
        model.addAttribute("categorias", categoriaService.obtenerTodas());
        return "proponer-lugar";
    }
}