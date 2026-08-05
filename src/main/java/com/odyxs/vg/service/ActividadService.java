package com.odyxs.vg.service;

import com.odyxs.vg.entity.Actividad;
import com.odyxs.vg.repository.ActividadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;

@Service
public class ActividadService {

    @Autowired private ActividadRepository actividadRepository;

    private static final String UPLOAD_DIR =
        System.getProperty("user.home") + "/odyxs-uploads/actividades/";
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    public List<Actividad> obtenerTodas() {
        return actividadRepository.findAll();
    }

    public List<Actividad> obtenerAprobadas() {
        return actividadRepository.findByEstado(Actividad.Estado.APROBADO);
    }

    public List<Actividad> obtenerPendientes() {
        return actividadRepository.findByEstado(Actividad.Estado.PENDIENTE);
    }

    public List<Actividad> obtenerPorCategoria(Actividad.CategoriaActividad cat) {
        return actividadRepository.findByCategoriaAndEstado(cat, Actividad.Estado.APROBADO);
    }

    public Actividad obtenerPorId(Long id) {
        return actividadRepository.findById(id).orElse(null);
    }

    public String aprobar(Long id) {
        Actividad a = actividadRepository.findById(id).orElse(null);
        if (a == null) return "Actividad no encontrada.";
        a.setEstado(Actividad.Estado.APROBADO);
        actividadRepository.save(a);
        return "Actividad aprobada.";
    }

    public String rechazar(Long id) {
        Actividad a = actividadRepository.findById(id).orElse(null);
        if (a == null) return "Actividad no encontrada.";
        a.setEstado(Actividad.Estado.RECHAZADO);
        actividadRepository.save(a);
        return "Actividad rechazada.";
    }

    /** Solo intenta borrar si la URL es una imagen subida localmente */
    private void borrarImagenLocal(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) return;
        if (!imagenUrl.startsWith("/uploads/")) return; // URL externa, no tocar
        try {
            Files.deleteIfExists(Paths.get(System.getProperty("user.home") + "/odyxs-uploads" + imagenUrl.replace("/uploads", "")));
        } catch (IOException e) {
            // continuar aunque falle el borrado
        }
    }

    public String eliminar(Long id) {
        Actividad a = actividadRepository.findById(id).orElse(null);
        if (a == null) return "Actividad no encontrada.";
        borrarImagenLocal(a.getImagenUrl());
        actividadRepository.deleteById(id);
        return "Actividad eliminada.";
    }

    public String guardar(String nombre, String descripcion, String duracion,
                          String precioAprox, String categoriaStr,
                          boolean esOficial, MultipartFile imagen) {
        try {
            Actividad.CategoriaActividad cat =
                Actividad.CategoriaActividad.valueOf(categoriaStr.toUpperCase());

            String imagenGuardada = null;
            if (imagen != null && !imagen.isEmpty()) {
                imagenGuardada = guardarArchivo(imagen);
                if (imagenGuardada == null) return "Error al guardar la imagen.";
            }

            Actividad a = new Actividad();
            a.setNombre(nombre);
            a.setDescripcion(descripcion);
            a.setDuracion(duracion);
            a.setPrecioAprox(precioAprox);
            a.setCategoria(cat);
            a.setEsOficial(esOficial);
            a.setEstado(esOficial ? Actividad.Estado.APROBADO : Actividad.Estado.PENDIENTE);
            a.setImagenUrl(imagenGuardada);
            actividadRepository.save(a);
            return "Actividad guardada.";
        } catch (IllegalArgumentException e) {
            return "Categoría inválida.";
        }
    }

    public String actualizar(Long id, String nombre, String descripcion, String duracion,
                             String precioAprox, String categoriaStr, MultipartFile imagen) {
        Actividad a = actividadRepository.findById(id).orElse(null);
        if (a == null) return "Actividad no encontrada.";
        try {
            a.setNombre(nombre);
            a.setDescripcion(descripcion);
            a.setDuracion(duracion);
            a.setPrecioAprox(precioAprox);
            a.setCategoria(Actividad.CategoriaActividad.valueOf(categoriaStr.toUpperCase()));
            if (imagen != null && !imagen.isEmpty()) {
                String url = guardarArchivo(imagen);
                if (url != null) {
                    borrarImagenLocal(a.getImagenUrl()); // borrar imagen vieja solo si es local
                    a.setImagenUrl(url);
                }
            }
            actividadRepository.save(a);
            return "Actividad actualizada.";
        } catch (IllegalArgumentException e) {
            return "Categoría inválida.";
        }
    }

    private String guardarArchivo(MultipartFile imagen) {
        if (imagen.getSize() > MAX_BYTES) return null;
        String tipo = imagen.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) return null;
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            // Sanitizar nombre: reemplazar espacios y caracteres especiales con guiones
            String nombreOriginal = imagen.getOriginalFilename() != null
                    ? imagen.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "-")
                    : "imagen.jpg";
            String nombreArchivo = System.currentTimeMillis() + "_" + nombreOriginal;
            Path destino = Paths.get(UPLOAD_DIR + nombreArchivo);
            Files.copy(imagen.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/actividades/" + nombreArchivo;
        } catch (IOException e) {
            return null;
        }
    }
}
