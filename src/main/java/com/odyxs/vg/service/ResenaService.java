package com.odyxs.vg.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.entity.Resenas;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.repository.ResenaRepository;
import com.odyxs.vg.repository.UsuarioRepository;

@Service
public class ResenaService {

    @Autowired
    private ResenaRepository resenaRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    public String guardar(Long lugarId, Long usuarioId, String comentario, Integer calificacion) {
        Lugar lugar = lugarRepository.findById(lugarId).orElse(null);
        Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);
        if (lugar == null || usuario == null) {
            return "Lugar o usuario no encontrado.";
        }
        if (calificacion < 1 || calificacion > 5) {
            return "La calificación debe estar entre 1 y 5.";
        }
        Resenas resena = new Resenas();
        resena.setLugar(lugar);
        resena.setUsuario(usuario);
        resena.setComentario(comentario);
        resena.setCalificacion(calificacion);
        resena.setFecha(LocalDate.now());
        resenaRepository.save(resena);
        return "Reseña guardada.";
    }

    public String editar(Long resenaId, Long usuarioId, String comentario, Integer calificacion) {
        Resenas resena = resenaRepository.findById(resenaId).orElse(null);
        if (resena == null) return "Reseña no encontrada.";
        if (!resena.getUsuario().getId().equals(usuarioId)) return "No tienes permiso para editar esta reseña.";
        if (calificacion < 1 || calificacion > 5) return "La calificación debe estar entre 1 y 5.";
        resena.setComentario(comentario);
        resena.setCalificacion(calificacion);
        resenaRepository.save(resena);
        return "Reseña actualizada.";
    }

    public String eliminar(Long resenaId, Long usuarioId) {
        Resenas resena = resenaRepository.findById(resenaId).orElse(null);
        if (resena == null) return "Reseña no encontrada.";
        if (!resena.getUsuario().getId().equals(usuarioId)) return "No tienes permiso para eliminar esta reseña.";
        resenaRepository.deleteById(resenaId);
        return "Reseña eliminada.";
    }

    public List<Resenas> obtenerPorLugar(Long lugarId) {
        return resenaRepository.findByLugarId(lugarId);
    }

    public Resenas obtenerPorId(Long id) {
        return resenaRepository.findById(id).orElse(null);
    }
}