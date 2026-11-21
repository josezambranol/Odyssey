package com.odyxs.vg.service;

import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.CategoriaRepository;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.repository.ResenaRepository;
import com.odyxs.vg.repository.SuscripcionLugarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class LugarService {

    @Autowired private LugarRepository lugarRepository;
    @Autowired private CategoriaRepository categoriaRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired(required = false) private ResenaRepository resenaRepository;
    @Autowired(required = false) private SuscripcionLugarRepository suscripcionLugarRepository;

    public List<Lugar> obtenerTodos() {
        return lugarRepository.findAll();
    }

    public Lugar obtenerPorId(Long id) {
        return lugarRepository.findById(id).orElse(null);
    }

    public List<Lugar> obtenerPorCategoria(Long categoriaId) {
        return lugarRepository.findByCategoriaId(categoriaId);
    }

    @Cacheable(value = "lugares", key = "'aprobados'")
    public List<Lugar> obtenerAprobados() {
        return lugarRepository.findByEstado(Lugar.Estado.APROBADO);
    }

    public List<Lugar> obtenerPendientes() {
        return lugarRepository.findByEstado(Lugar.Estado.PENDIENTE);
    }

    public List<Lugar> obtenerPorCategoriaAprobados(Long categoriaId) {
        return lugarRepository.findByCategoriaIdAndEstado(categoriaId, Lugar.Estado.APROBADO);
    }

    @CacheEvict(value = "lugares", allEntries = true)
    public String aprobar(Long id) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        lugar.setEstado(Lugar.Estado.APROBADO);
        lugarRepository.save(lugar);
        return "Lugar aprobado.";
    }

    @CacheEvict(value = "lugares", allEntries = true)
    public String rechazar(Long id) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        lugar.setEstado(Lugar.Estado.RECHAZADO);
        lugarRepository.save(lugar);
        return "Lugar rechazado.";
    }

    public List<Lugar> buscar(String texto) {
        return lugarRepository.buscarPorTexto(texto.trim(), Lugar.Estado.APROBADO);
    }

    @Transactional
    @CacheEvict(value = "lugares", allEntries = true)
    public String eliminar(Long id) {
        Lugar lugar = lugarRepository.findById(id).orElse(null);
        if (lugar == null) return "Lugar no encontrado.";
        if (resenaRepository != null) {
            resenaRepository.deleteByLugarId(id);
        }
        if (suscripcionLugarRepository != null) {
            suscripcionLugarRepository.deleteByLugarId(id);
        }
        fileStorageService.borrarArchivo(lugar.getImagenUrl());
        lugarRepository.deleteById(id);
        return "Lugar eliminado.";
    }

    @CacheEvict(value = "lugares", allEntries = true)
    public String guardar(Long categoriaId, String nombre, String descripcion,
                          String ubicacion, String urlMapa, boolean esOficial,
                          MultipartFile imagen) {
        return guardar(categoriaId, nombre, descripcion, ubicacion, urlMapa, esOficial, imagen, null, null, null);
    }

    @CacheEvict(value = "lugares", allEntries = true)
    public String guardar(Long categoriaId, String nombre, String descripcion,
                          String ubicacion, String urlMapa, boolean esOficial,
                          MultipartFile imagen, String imagenLocal) {
        return guardar(categoriaId, nombre, descripcion, ubicacion, urlMapa, esOficial, imagen, imagenLocal, null, null);
    }

    @CacheEvict(value = "lugares", allEntries = true)
    public String guardar(Long categoriaId, String nombre, String descripcion,
                          String ubicacion, String urlMapa, boolean esOficial,
                          MultipartFile imagen, String imagenLocal,
                          Double latitud, Double longitud) {

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
        lugar.setLatitud(latitud);
        lugar.setLongitud(longitud);
        lugarRepository.save(lugar);
        return "Lugar guardado.";
    }

    @CacheEvict(value = "lugares", allEntries = true)
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
