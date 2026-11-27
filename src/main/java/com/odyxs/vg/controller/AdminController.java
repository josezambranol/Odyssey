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
import com.odyxs.vg.service.WebPushNotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    @Autowired(required = false) private WebPushNotificationService pushNotificationService;

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
                               @RequestParam(required = false) Double latitud,
                               @RequestParam(required = false) Double longitud,
                               @RequestParam Long categoriaId,
                               @RequestParam(required = false) MultipartFile imagen,
                               @RequestParam(required = false) String imagenLocal,
                               RedirectAttributes redirectAttributes) {
        String resultado = lugarService.guardar(categoriaId, nombre, descripcion, ubicacion, urlMapa, true, imagen, imagenLocal, latitud, longitud);
        if ("Lugar guardado.".equals(resultado)) {
            redirectAttributes.addFlashAttribute("mensajeAdmin", "Lugar creado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("errorAdmin", resultado);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/{id}/editar")
    @CacheEvict(value = "lugares", allEntries = true)
    public String editarLugar(@PathVariable Long id,
                              @RequestParam String nombre,
                              @RequestParam String descripcion,
                              @RequestParam String ubicacion,
                              @RequestParam(required = false) String urlMapa,
                              @RequestParam(required = false) Double latitud,
                              @RequestParam(required = false) Double longitud,
                              @RequestParam Long categoriaId,
                              @RequestParam(required = false) MultipartFile imagen,
                              @RequestParam(required = false) String imagenUrlExterna,
                              RedirectAttributes redirectAttributes) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) {
            redirectAttributes.addFlashAttribute("errorAdmin", "Lugar no encontrado.");
            return "redirect:/admin";
        }
        lugar.setNombre(nombre.trim());
        lugar.setDescripcion(descripcion != null ? descripcion.trim() : null);
        lugar.setUbicacion(ubicacion.trim());
        lugar.setUrlMapa(urlMapa != null ? urlMapa.trim() : null);
        lugar.setLatitud(latitud);
        lugar.setLongitud(longitud);
        Categoria cat = categoriaRepository.findById(categoriaId).orElse(null);
        if (cat != null) lugar.setCategoria(cat);

        if (imagen != null && !imagen.isEmpty()) {
            String url = fileStorageService.guardarImagen(imagen, "lugares");
            if (url != null) {
                fileStorageService.borrarArchivo(lugar.getImagenUrl());
                lugar.setImagenUrl(url);
            } else {
                redirectAttributes.addFlashAttribute("errorAdmin", "La imagen no es válida (solo JPG/PNG/WEBP/GIF, máx. 5 MB).");
                return "redirect:/admin";
            }
        } else if (imagenUrlExterna != null && !imagenUrlExterna.isBlank()) {
            String limpia = imagenUrlExterna.trim();
            if (limpia.startsWith("https://") || limpia.startsWith("http://")
                    || limpia.startsWith("/img/") || limpia.startsWith("/uploads/")) {
                fileStorageService.borrarArchivo(lugar.getImagenUrl());
                lugar.setImagenUrl(limpia);
            } else {
                redirectAttributes.addFlashAttribute("errorAdmin", "La URL de imagen externa no tiene un formato permitido.");
                return "redirect:/admin";
            }
        }

        lugarRepository.save(lugar);
        redirectAttributes.addFlashAttribute("mensajeAdmin", "Lugar actualizado correctamente.");
        return "redirect:/admin";
    }

    // ── EVENTOS ───────────────────────────────────────────────

    @PostMapping("/admin/eventos/agregar")
    @CacheEvict(value = "eventosClasificados", allEntries = true)
    public String agregarEvento(@RequestParam String nombre,
                                @RequestParam String descripcion,
                                @RequestParam String fecha,
                                @RequestParam(required = false) String lugar,
                                @RequestParam(required = false) MultipartFile imagen,
                                RedirectAttributes redirectAttributes) {
        try {
            Evento e = new Evento();
            e.setNombre(nombre.trim());
            e.setDescripcion(descripcion != null ? descripcion.trim() : null);
            e.setFecha(LocalDate.parse(fecha.trim()));
            e.setLugar(lugar != null ? lugar.trim() : null);
            if (imagen != null && !imagen.isEmpty()) {
                String url = fileStorageService.guardarImagen(imagen, "eventos");
                if (url == null) {
                    redirectAttributes.addFlashAttribute("errorAdmin", "Imagen de evento inválida (solo JPG/PNG/WEBP/GIF, máx. 5 MB).");
                    return "redirect:/admin";
                }
                e.setImagenUrl(url);
            }
            e.setActivo(true);
            eventoRepo.save(e);
            if (pushNotificationService != null) {
                pushNotificationService.publicarAlertaEvento(
                        "🎭 Nuevo evento: " + e.getNombre(),
                        (e.getLugar() != null ? e.getLugar() + " · " : "") + e.getFecha(),
                        "/eventos",
                        null
                );
            }
            redirectAttributes.addFlashAttribute("mensajeAdmin", "Evento creado correctamente.");
        } catch (DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("errorAdmin", "Formato de fecha de evento no válido.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/aprobar")
    @CacheEvict(value = "eventosClasificados", allEntries = true)
    public String aprobarEvento(@PathVariable Long id) {
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) {
            e.setActivo(true);
            eventoRepo.save(e);
            if (pushNotificationService != null) {
                pushNotificationService.publicarAlertaEvento(
                        "🎉 Evento confirmado: " + e.getNombre(),
                        (e.getLugar() != null ? e.getLugar() + " · " : "") + e.getFecha(),
                        "/eventos",
                        null
                );
            }
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/rechazar")
    @CacheEvict(value = "eventosClasificados", allEntries = true)
    public String rechazarEvento(@PathVariable Long id) {
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) {
            fileStorageService.borrarArchivo(e.getImagenUrl());
            eventoRepo.deleteById(id);
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/editar")
    @CacheEvict(value = "eventosClasificados", allEntries = true)
    public String editarEvento(@PathVariable Long id,
                               @RequestParam String nombre,
                               @RequestParam String descripcion,
                               @RequestParam String fecha,
                               @RequestParam(required = false) String lugar,
                               @RequestParam(required = false) MultipartFile imagen,
                               RedirectAttributes redirectAttributes) {
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e == null) {
            redirectAttributes.addFlashAttribute("errorAdmin", "Evento no encontrado.");
            return "redirect:/admin";
        }
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
                } else {
                    redirectAttributes.addFlashAttribute("errorAdmin", "Imagen de evento inválida (solo JPG/PNG/WEBP/GIF, máx. 5 MB).");
                    return "redirect:/admin";
                }
            }
            eventoRepo.save(e);
            redirectAttributes.addFlashAttribute("mensajeAdmin", "Evento actualizado correctamente.");
        } catch (DateTimeParseException ex) {
            redirectAttributes.addFlashAttribute("errorAdmin", "Formato de fecha de evento no válido.");
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/eliminar")
    @CacheEvict(value = "eventosClasificados", allEntries = true)
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
                                   @RequestParam(required = false) MultipartFile imagen,
                                   RedirectAttributes redirectAttributes) {
        String res = actividadService.guardar(nombre, descripcion, duracion, precioAprox, categoria, true, imagen);
        if ("Actividad guardada.".equals(res)) {
            redirectAttributes.addFlashAttribute("mensajeAdmin", res);
        } else {
            redirectAttributes.addFlashAttribute("errorAdmin", res);
        }
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
                                  @RequestParam(required = false) MultipartFile imagen,
                                  RedirectAttributes redirectAttributes) {
        String res = actividadService.actualizar(id, nombre, descripcion, duracion, precioAprox, categoria, imagen);
        if ("Actividad actualizada.".equals(res)) {
            redirectAttributes.addFlashAttribute("mensajeAdmin", res);
        } else {
            redirectAttributes.addFlashAttribute("errorAdmin", res);
        }
        return "redirect:/admin";
    }
}