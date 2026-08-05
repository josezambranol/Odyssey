package com.odyxs.vg.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.CategoriaRepository;
import com.odyxs.vg.repository.LugarRepository;

@Service
public class LugarService {

    @Autowired private LugarRepository lugarRepository;
    @Autowired private CategoriaRepository categoriaRepository;

    private static final String UPLOAD_DIR = System.getProperty("user.home") + "/odyxs-uploads/lugares/";
    private static final long MAX_BYTES = 2 * 1024 * 1024;

    public List<Lugar> obtenerTodos() {
        return lugarRepository.findAll();
    }

    public Lugar obtenerPorId(Long id) {
        return lugarRepository.findById(id).orElse(null);
    }

    public List<Lugar> obtenerPorCategoria(Long categoriaId) {
        return lugarRepository.findByCategoriaId(categoriaId);
    }

    public List<Lugar> obtenerAprobados() {
        return lugarRepository.findByEstado(Lugar.Estado.APROBADO);
    }

    public List<Lugar> obtenerPendientes() {
        return lugarRepository.findByEstado(Lugar.Estado.PENDIENTE);
    }

    public List<Lugar> obtenerPorCategoriaAprobados(Long categoriaId) {
        return lugarRepository.findByCategoriaIdAndEstado(categoriaId, Lugar.Estado.APROBADO);
    }


    public String aprobar(Long id) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        lugar.setEstado(Lugar.Estado.APROBADO);
        lugarRepository.save(lugar);
        return "Lugar aprobado.";
    }

    public String rechazar(Long id) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        lugar.setEstado(Lugar.Estado.RECHAZADO);
        lugarRepository.save(lugar);
        return "Lugar rechazado.";
    }

    public List<Lugar> buscar(String texto) {
        return lugarRepository.findByNombreContainingIgnoreCaseAndEstado(texto, Lugar.Estado.APROBADO);
    }


    /** Solo intenta borrar si la URL es una imagen subida localmente */
    private void borrarImagenLocal(String imagenUrl) {
        if (imagenUrl == null || imagenUrl.isBlank()) return;
        if (!imagenUrl.startsWith("/uploads/")) return; // URL externa, no tocar
        try {
            Path imgPath = Paths.get(System.getProperty("user.home") + "/odyxs-uploads" + imagenUrl.replace("/uploads", ""));
            Files.deleteIfExists(imgPath);
        } catch (IOException e) {
            // Si falla el borrado del archivo, continuamos igual
        }
    }

    public String eliminar(Long id) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        borrarImagenLocal(lugar.getImagenUrl());
        lugarRepository.deleteById(id);
        return "Lugar eliminado.";
    }

    public String guardar(Long categoriaId, String nombre, String descripcion,
                          String ubicacion, String urlMapa, boolean esOficial,
                          MultipartFile imagen) {
        return guardar(categoriaId, nombre, descripcion, ubicacion, urlMapa, esOficial, imagen, null);
    }

    public String guardar(Long categoriaId, String nombre, String descripcion,
                          String ubicacion, String urlMapa, boolean esOficial,
                          MultipartFile imagen, String imagenLocal) {

        Categoria categoria = categoriaRepository.findById(categoriaId).orElse(null);
        if (categoria == null) return "Categoría no encontrada.";

        if (lugarRepository.existsByNombreAndDescripcionAndUbicacionAndUrlMapa(
                nombre, descripcion, ubicacion, urlMapa)) {
            return "Ya existe un lugar con el mismo nombre, descripción, ubicación y URL.";
        }

        String imagenUrlGuardada = null;
        if (imagen != null && !imagen.isEmpty()) {
            imagenUrlGuardada = guardarArchivo(imagen);
            if (imagenUrlGuardada == null) return "Error al guardar la imagen.";
        }

        Lugar lugar = new Lugar();
        lugar.setNombre(nombre);
        lugar.setDescripcion(descripcion);
        lugar.setUbicacion(ubicacion);
        lugar.setUrlMapa(urlMapa);
        lugar.setEsOficial(esOficial);
        lugar.setEstado(esOficial ? Lugar.Estado.APROBADO : Lugar.Estado.PENDIENTE);
        lugar.setCategoria(categoria);
        lugar.setImagenUrl(imagenUrlGuardada);
        lugarRepository.save(lugar);
        return "Lugar guardado.";
    }

    public String actualizarImagen(Long id, MultipartFile imagen) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        if (imagen == null || imagen.isEmpty()) return "No se envió imagen.";

        String imagenUrl = guardarArchivo(imagen);
        if (imagenUrl == null) return "Error al guardar la imagen.";

        // Borrar imagen vieja solo si es local
        borrarImagenLocal(lugar.getImagenUrl());

        lugar.setImagenUrl(imagenUrl);
        lugarRepository.save(lugar);
        return "Imagen actualizada.";
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
            return "/uploads/lugares/" + nombreArchivo;
        } catch (IOException e) {
            return null;
        }
    }
}