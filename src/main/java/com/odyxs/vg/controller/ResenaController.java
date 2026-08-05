package com.odyxs.vg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.odyxs.vg.entity.Resenas;
import com.odyxs.vg.service.ResenaService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    @PostMapping("/resenas")
    public String guardarResena(@RequestParam Long lugarId,
                                @RequestParam String comentario,
                                @RequestParam Integer calificacion,
                                HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        resenaService.guardar(lugarId, usuarioId, comentario, calificacion);
        return "redirect:/lugares/" + lugarId;
    }

    @PostMapping("/resenas/{id}/editar")
    public String editarResena(@PathVariable Long id,
                               @RequestParam String comentario,
                               @RequestParam Integer calificacion,
                               HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Resenas resena = resenaService.obtenerPorId(id);
        if (resena == null) return "redirect:/menu";
        Long lugarId = resena.getLugar().getId();
        resenaService.editar(id, usuarioId, comentario, calificacion);
        return "redirect:/lugares/" + lugarId;
    }

    @PostMapping("/resenas/{id}/eliminar")
    public String eliminarResena(@PathVariable Long id, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        Long usuarioId = (Long) session.getAttribute("usuarioId");
        Resenas resena = resenaService.obtenerPorId(id);
        if (resena == null) return "redirect:/menu";
        Long lugarId = resena.getLugar().getId();
        resenaService.eliminar(id, usuarioId);
        return "redirect:/lugares/" + lugarId;
    }
}