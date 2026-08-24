package com.odyxs.vg.service;

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
    @Autowired private FileStorageService fileStorageService;

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
        return lugarRepository.findByNombreContainingIgnoreCaseAndEstado(texto.trim(), Lugar.Estado.APROBADO);
    }

    public String eliminar(Long id) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        fileStorageService.borrarArchivo(lugar.getImagenUrl());
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
            imagenUrlGuardada = fileStorageService.guardarImagen(imagen, "lugares");
            if (imagenUrlGuardada == null) return "Error al guardar la imagen (formato no soportado o tamaño excedido).";
        } else if (imagenLocal != null && !imagenLocal.isBlank()) {
            imagenUrlGuardada = imagenLocal.trim();
        }

        Lugar lugar = new Lugar();
        lugar.setNombre(nombre.trim());
        lugar.setDescripcion(descripcion != null ? descripcion.trim() : null);
        lugar.setUbicacion(ubicacion.trim());
        lugar.setUrlMapa(urlMapa != null ? urlMapa.trim() : null);
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

        String imagenUrl = fileStorageService.guardarImagen(imagen, "lugares");
        if (imagenUrl == null) return "Error al guardar la imagen.";

        fileStorageService.borrarArchivo(lugar.getImagenUrl());
        lugar.setImagenUrl(imagenUrl);
        lugarRepository.save(lugar);
        return "Imagen actualizada.";
    }
}