package com.odyxs.vg.controller;

import com.odyxs.vg.dto.ResenaDto;
import com.odyxs.vg.entity.Resenas;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.UsuarioRepository;
import com.odyxs.vg.security.CustomUserDetails;
import com.odyxs.vg.service.ResenaService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ResenaController {

    @Autowired
    private ResenaService resenaService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Long obtenerUsuarioId(CustomUserDetails userDetails, HttpSession session) {
        if (userDetails != null) {
            return userDetails.getId();
        }
        Object idSesion = session.getAttribute("usuarioId");
        if (idSesion instanceof Long l) return l;
        if (idSesion instanceof Integer i) return i.longValue();
        return null;
    }

    @PostMapping("/resenas")
    public String guardarResena(@RequestParam Long lugarId,
                                @RequestParam String comentario,
                                @RequestParam Integer calificacion,
                                @AuthenticationPrincipal CustomUserDetails userDetails,
                                HttpSession session) {
        Long usuarioId = obtenerUsuarioId(userDetails, session);
        if (usuarioId == null) return "redirect:/login";

        resenaService.guardar(lugarId, usuarioId, comentario != null ? comentario.trim() : "", calificacion);
        return "redirect:/lugares/" + lugarId;
    }

    @PostMapping("/resenas/{id}/editar")
    public String editarResena(@PathVariable Long id,
                               @RequestParam String comentario,
                               @RequestParam Integer calificacion,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpSession session) {
        Long usuarioId = obtenerUsuarioId(userDetails, session);
        if (usuarioId == null) return "redirect:/login";

        Resenas resena = resenaService.obtenerPorId(id);
        if (resena == null) return "redirect:/lugares";
        Long lugarId = resena.getLugar().getId();
        resenaService.editar(id, usuarioId, comentario != null ? comentario.trim() : "", calificacion);
        return "redirect:/lugares/" + lugarId;
    }

    @PostMapping("/resenas/{id}/eliminar")
    public String eliminarResena(@PathVariable Long id,
                                 @AuthenticationPrincipal CustomUserDetails userDetails,
                                 HttpSession session) {
        Long usuarioId = obtenerUsuarioId(userDetails, session);
        if (usuarioId == null) return "redirect:/login";

        Resenas resena = resenaService.obtenerPorId(id);
        if (resena == null) return "redirect:/lugares";
        Long lugarId = resena.getLugar().getId();
        resenaService.eliminar(id, usuarioId);
        return "redirect:/lugares/" + lugarId;
    }
}