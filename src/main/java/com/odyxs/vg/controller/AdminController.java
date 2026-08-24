package com.odyxs.vg.controller;

import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.repository.CategoriaRepository;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.service.ActividadService;
import com.odyxs.vg.service.CategoriaService;
import com.odyxs.vg.service.FileStorageService;
import com.odyxs.vg.service.LugarService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired private LugarService      lugarService;
    @Autowired private LugarRepository   lugarRepository;
    @Autowired private CategoriaService  categoriaService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private EventoRepository  eventoRepo;
    @Autowired private ActividadService  actividadService;
    @Autowired private FileStorageService fileStorageService;

    @GetMapping("/admin")
    public String panelAdmin(Model model) {
        model.addAttribute("pendientes",           lugarService.obtenerPendientes());
        model.addAttribute("lugares",              lugarService.obtenerTodos());
        model.addAttribute("categorias",           categoriaService.obtenerTodas());
        model.addAttribute("eventos",              eventoRepo.findByActivoTrueOrderByFechaAsc());
        model.addAttribute("eventosPendientes",    eventoRepo.findByActivoFalseOrderByIdDesc());
        model.addAttribute("actividades",          actividadService.obtenerTodas());
        model.addAttribute("actividadesPendientes",actividadService.obtenerPendientes());
        return "admin";
    }

    // ── LUGARES ───────────────────────────────────────────────

    @PostMapping("/admin/lugares/{id}/aprobar")
    public String aprobar(@PathVariable Long id) {
        lugarService.aprobar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/{id}/rechazar")
    public String rechazar(@PathVariable Long id) {
        lugarService.rechazar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/{id}/eliminar")
    public String eliminarLugar(@PathVariable Long id) {
        lugarService.eliminar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/agregar")
    public String agregarLugar(@RequestParam String nombre,
                               @RequestParam String descripcion,
                               @RequestParam String ubicacion,
                               @RequestParam(required = false) String urlMapa,
                               @RequestParam Long categoriaId,
                               @RequestParam(required = false) MultipartFile imagen,
                               @RequestParam(required = false) String imagenLocal) {
        lugarService.guardar(categoriaId, nombre, descripcion, ubicacion, urlMapa, true, imagen, imagenLocal);
        return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/{id}/editar")
    public String editarLugar(@PathVariable Long id,
                              @RequestParam String nombre,
                              @RequestParam String descripcion,
                              @RequestParam String ubicacion,
                              @RequestParam(required = false) String urlMapa,
                              @RequestParam Long categoriaId,
                              @RequestParam(required = false) MultipartFile imagen,
                              @RequestParam(required = false) String imagenUrlExterna) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "redirect:/admin";
        lugar.setNombre(nombre.trim());
        lugar.setDescripcion(descripcion != null ? descripcion.trim() : null);
        lugar.setUbicacion(ubicacion.trim());
        lugar.setUrlMapa(urlMapa != null ? urlMapa.trim() : null);
        Categoria cat = categoriaRepository.findById(categoriaId).orElse(null);
        if (cat != null) lugar.setCategoria(cat);

        if (imagen != null && !imagen.isEmpty()) {
            String url = fileStorageService.guardarImagen(imagen, "lugares");
            if (url != null) {
                fileStorageService.borrarArchivo(lugar.getImagenUrl());
                lugar.setImagenUrl(url);
            }
        } else if (imagenUrlExterna != null && !imagenUrlExterna.isBlank()) {
            fileStorageService.borrarArchivo(lugar.getImagenUrl());
            lugar.setImagenUrl(imagenUrlExterna.trim());
        }

        lugarRepository.save(lugar);
        return "redirect:/admin";
    }

    // ── EVENTOS ───────────────────────────────────────────────

    @PostMapping("/admin/eventos/agregar")
    public String agregarEvento(@RequestParam String nombre,
                                @RequestParam String descripcion,
                                @RequestParam String fecha,
                                @RequestParam(required = false) String lugar,
                                @RequestParam(required = false) MultipartFile imagen) {
        try {
            Evento e = new Evento();
            e.setNombre(nombre.trim());
            e.setDescripcion(descripcion != null ? descripcion.trim() : null);
            e.setFecha(LocalDate.parse(fecha.trim()));
            e.setLugar(lugar != null ? lugar.trim() : null);
            e.setImagenUrl(fileStorageService.guardarImagen(imagen, "eventos"));
            e.setActivo(true);
            eventoRepo.save(e);
        } catch (DateTimeParseException ignored) {
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/aprobar")
    public String aprobarEvento(@PathVariable Long id) {
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) {
            e.setActivo(true);
            eventoRepo.save(e);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/rechazar")
    public String rechazarEvento(@PathVariable Long id) {
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) {
            fileStorageService.borrarArchivo(e.getImagenUrl());
            eventoRepo.deleteById(id);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/editar")
    public String editarEvento(@PathVariable Long id,
                               @RequestParam String nombre,
                               @RequestParam String descripcion,
                               @RequestParam String fecha,
                               @RequestParam(required = false) String lugar,
                               @RequestParam(required = false) MultipartFile imagen) {
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e == null) return "redirect:/admin";
        try {
            e.setNombre(nombre.trim());
            e.setDescripcion(descripcion != null ? descripcion.trim() : null);
            e.setFecha(LocalDate.parse(fecha.trim()));
            e.setLugar(lugar != null ? lugar.trim() : null);
            if (imagen != null && !imagen.isEmpty()) {
                String url = fileStorageService.guardarImagen(imagen, "eventos");
                if (url != null) {
                    fileStorageService.borrarArchivo(e.getImagenUrl());
                    e.setImagenUrl(url);
                }
            }
            eventoRepo.save(e);
        } catch (DateTimeParseException ignored) {
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/eliminar")
    public String eliminarEvento(@PathVariable Long id) {
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) {
            fileStorageService.borrarArchivo(e.getImagenUrl());
            eventoRepo.deleteById(id);
        }
        return "redirect:/admin";
    }

    // ── ACTIVIDADES ───────────────────────────────────────────

    @PostMapping("/admin/actividades/agregar")
    public String agregarActividad(@RequestParam String nombre,
                                   @RequestParam(required = false) String descripcion,
                                   @RequestParam(required = false) String duracion,
                                   @RequestParam(required = false) String precioAprox,
                                   @RequestParam String categoria,
                                   @RequestParam(required = false) MultipartFile imagen) {
        actividadService.guardar(nombre, descripcion, duracion, precioAprox, categoria, true, imagen);
        return "redirect:/admin";
    }

    @PostMapping("/admin/actividades/{id}/aprobar")
    public String aprobarActividad(@PathVariable Long id) {
        actividadService.aprobar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/actividades/{id}/rechazar")
    public String rechazarActividad(@PathVariable Long id) {
        actividadService.rechazar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/actividades/{id}/eliminar")
    public String eliminarActividad(@PathVariable Long id) {
        actividadService.eliminar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/actividades/{id}/editar")
    public String editarActividad(@PathVariable Long id,
                                  @RequestParam String nombre,
                                  @RequestParam(required = false) String descripcion,
                                  @RequestParam(required = false) String duracion,
                                  @RequestParam(required = false) String precioAprox,
                                  @RequestParam String categoria,
                                  @RequestParam(required = false) MultipartFile imagen) {
        actividadService.actualizar(id, nombre, descripcion, duracion, precioAprox, categoria, imagen);
        return "redirect:/admin";
    }
}