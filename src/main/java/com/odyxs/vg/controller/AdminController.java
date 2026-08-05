package com.odyxs.vg.controller;

import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.repository.CategoriaRepository;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.service.ActividadService;
import com.odyxs.vg.service.CategoriaService;
import com.odyxs.vg.service.LugarService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;

@Controller
public class AdminController {

    @Autowired private LugarService      lugarService;
    @Autowired private LugarRepository   lugarRepository;
    @Autowired private CategoriaService  categoriaService;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private EventoRepository  eventoRepo;
    @Autowired private ActividadService  actividadService;

    private static final String UPLOAD_BASE = System.getProperty("user.home") + "/odyxs-uploads/";
    private static final String EVENTOS_DIR  = UPLOAD_BASE + "eventos/";
    private static final String LUGARES_DIR  = UPLOAD_BASE + "lugares/";

    /** Borra un archivo físico dado su URL relativa (/uploads/...). Ignora URLs externas. */
    private void borrarArchivoFisico(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) return;
        if (!imagenUrl.startsWith("/uploads/")) return; // URL externa (http/https), no tocar
        try {
            Path p = Paths.get(
                System.getProperty("user.home") + "/odyxs-uploads" + imagenUrl.replace("/uploads", ""));
            Files.deleteIfExists(p);
        } catch (IOException e) {
            // no detener el flujo si falla
        }
    }

    private boolean esAdmin(HttpSession session) {
        Object rol    = session.getAttribute("usuarioRol");
        Object correo = session.getAttribute("usuarioCorreo");
        return rol != null && rol.toString().equals("ADMIN")
            && "admin@odyxs.com".equals(correo);
    }

    @GetMapping("/admin")
    public String panelAdmin(HttpSession session, Model model) {
        if (!esAdmin(session)) return "redirect:/";
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
    public String aprobar(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        lugarService.aprobar(id); return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/{id}/rechazar")
    public String rechazar(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        lugarService.rechazar(id); return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/{id}/eliminar")
    public String eliminarLugar(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        lugarService.eliminar(id); return "redirect:/admin";
    }

    @PostMapping("/admin/lugares/agregar")
    public String agregarLugar(@RequestParam String nombre,
                               @RequestParam String descripcion,
                               @RequestParam String ubicacion,
                               @RequestParam(required = false) String urlMapa,
                               @RequestParam Long categoriaId,
                               @RequestParam(required = false) MultipartFile imagen,
                               @RequestParam(required = false) String imagenLocal,
                               HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
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
                              @RequestParam(required = false) String imagenUrlExterna,
                              HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "redirect:/admin";
        lugar.setNombre(nombre);
        lugar.setDescripcion(descripcion);
        lugar.setUbicacion(ubicacion);
        lugar.setUrlMapa(urlMapa);
        Categoria cat = categoriaRepository.findById(categoriaId).orElse(null);
        if (cat != null) lugar.setCategoria(cat);

        if (imagen != null && !imagen.isEmpty()) {
            // Prioridad 1: nueva imagen subida como archivo
            String url = guardarImagenLugar(imagen);
            if (url != null) {
                borrarArchivoFisico(lugar.getImagenUrl());
                lugar.setImagenUrl(url);
            }
        } else if (imagenUrlExterna != null && !imagenUrlExterna.isBlank()) {
            // Prioridad 2: URL externa pegada manualmente
            borrarArchivoFisico(lugar.getImagenUrl()); // solo borra si era local
            lugar.setImagenUrl(imagenUrlExterna.trim());
        }
        // Si ninguno se envió, se conserva la imagen actual

        lugarRepository.save(lugar);
        return "redirect:/admin";
    }

    // ── EVENTOS ───────────────────────────────────────────────

    private String guardarImagenEvento(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) return null;
        String tipo = imagen.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) return null;
        try {
            Files.createDirectories(Paths.get(EVENTOS_DIR));
            String ext = "";
            String original = imagen.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            } else {
                // Inferir extensión del content-type
                ext = tipo.equals("image/png") ? ".png"
                    : tipo.equals("image/webp") ? ".webp"
                    : tipo.equals("image/gif") ? ".gif"
                    : ".jpg";
            }
            String nombreArchivo = System.currentTimeMillis() + ext;
            Path destino = Paths.get(EVENTOS_DIR + nombreArchivo);
            Files.copy(imagen.getInputStream(), destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/eventos/" + nombreArchivo;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String guardarImagenLugar(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) return null;
        String tipo = imagen.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) return null;
        try {
            Files.createDirectories(Paths.get(LUGARES_DIR));
            String original = imagen.getOriginalFilename();
            String ext = (original != null && original.contains("."))
                ? original.substring(original.lastIndexOf("."))
                : (tipo.equals("image/png") ? ".png" : tipo.equals("image/webp") ? ".webp" : ".jpg");
            String nombreArchivo = System.currentTimeMillis() + ext;
            Path destino = Paths.get(LUGARES_DIR + nombreArchivo);
            Files.copy(imagen.getInputStream(), destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/lugares/" + nombreArchivo;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @PostMapping("/admin/eventos/agregar")
    public String agregarEvento(@RequestParam String nombre,
                                @RequestParam String descripcion,
                                @RequestParam String fecha,
                                @RequestParam(required = false) String lugar,
                                @RequestParam(required = false) MultipartFile imagen,
                                HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Evento e = new Evento();
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        e.setFecha(java.time.LocalDate.parse(fecha));
        e.setLugar(lugar);
        e.setImagenUrl(guardarImagenEvento(imagen));
        e.setActivo(true);
        eventoRepo.save(e);
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/aprobar")
    public String aprobarEvento(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) { e.setActivo(true); eventoRepo.save(e); }
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/rechazar")
    public String rechazarEvento(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) {
            borrarArchivoFisico(e.getImagenUrl());
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
                               @RequestParam(required = false) MultipartFile imagen,
                               HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e == null) return "redirect:/admin";
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        e.setFecha(java.time.LocalDate.parse(fecha));
        e.setLugar(lugar);
        String url = guardarImagenEvento(imagen);
        if (url != null) {
            borrarArchivoFisico(e.getImagenUrl()); // borrar imagen vieja
            e.setImagenUrl(url);
        }
        eventoRepo.save(e);
        return "redirect:/admin";
    }

    @PostMapping("/admin/eventos/{id}/eliminar")
    public String eliminarEvento(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        Evento e = eventoRepo.findById(id).orElse(null);
        if (e != null) {
            borrarArchivoFisico(e.getImagenUrl());
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
                                   HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        actividadService.guardar(nombre, descripcion, duracion, precioAprox, categoria, true, imagen);
        return "redirect:/admin";
    }

    @PostMapping("/admin/actividades/{id}/aprobar")
    public String aprobarActividad(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        actividadService.aprobar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/actividades/{id}/rechazar")
    public String rechazarActividad(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        actividadService.rechazar(id);
        return "redirect:/admin";
    }

    @PostMapping("/admin/actividades/{id}/eliminar")
    public String eliminarActividad(@PathVariable Long id, HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
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
                                  HttpSession session) {
        if (!esAdmin(session)) return "redirect:/";
        actividadService.actualizar(id, nombre, descripcion, duracion, precioAprox, categoria, imagen);
        return "redirect:/admin";
    }
}
