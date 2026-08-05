package com.odyxs.vg.controller;

import com.odyxs.vg.entity.*;
import com.odyxs.vg.service.*;
import jakarta.servlet.http.HttpSession;
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

    // ── Listado por categoria ─────────────────────────────────
    @GetMapping("/lugares")
    public String lugares(@RequestParam(required = false) Long categoriaId,
                          Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        List<Lugar> lista = (categoriaId == null || categoriaId == 0)
                ? lugarService.obtenerAprobados()
                : lugarService.obtenerPorCategoriaAprobados(categoriaId);

        // catNombre: nombre original de la categoría (para el título de la página)
        // catNombreClave: clave sanitizada para i18n (sin tildes ni espacios)
        String catNombre = null;
        String catNombreClave = null;
        if (!lista.isEmpty() && categoriaId != null && categoriaId != 0) {
            catNombre = lista.get(0).getCategoria().getNombre();
            catNombreClave = lista.get(0).getCategoria().getNombreClave();
        } else if (categoriaId != null && categoriaId != 0) {
            // Lista vacía pero hay filtro de categoría: buscar la categoría directamente
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

    // ── Detalle ───────────────────────────────────────────────
    @GetMapping("/lugares/{id}")
    public String detalle(@PathVariable Long id, Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        Lugar lugar = lugarService.obtenerPorId(id);
        if (lugar == null) return "redirect:/menu";
        model.addAttribute("lugar",   lugar);
        model.addAttribute("resenas", resenaService.obtenerPorLugar(id));
        return "detalle";
    }

    // ── Busqueda global ───────────────────────────────────────
    @GetMapping("/buscar")
    public String buscar(@RequestParam(defaultValue = "") String q,
                         Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        List<Lugar> resultados = q.isBlank()
                ? lugarService.obtenerAprobados()
                : lugarService.buscar(q);
        model.addAttribute("lugares", resultados);
        model.addAttribute("query",   q);
        return "buscar";
    }

    // ── Proponer lugar ────────────────────────────────────────
    @GetMapping("/proponer-lugar")
    public String proponerForm(Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
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
                                Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        String resultado = lugarService.guardar(categoriaId, nombre, descripcion,
                                                ubicacion, urlMapa, false, imagen);

        if (resultado.equals("Lugar guardado.")) {
            model.addAttribute("mensaje", "¡Propuesta enviada! Será revisada por el equipo ODYXS.");
        } else {
            model.addAttribute("error", resultado);
        }
        model.addAttribute("categorias", categoriaService.obtenerTodas());
        return "proponer-lugar";
    }
}
