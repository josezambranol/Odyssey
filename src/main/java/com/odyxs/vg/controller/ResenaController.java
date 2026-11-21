package com.odyxs.vg.controller;

import com.odyxs.vg.entity.Resenas;
import com.odyxs.vg.security.CustomUserDetails;
import com.odyxs.vg.service.ResenaService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ResenaController {

    private static final int MAX_COMENTARIO_LENGTH = 500;

    @Autowired
    private ResenaService resenaService;

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
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        Long usuarioId = obtenerUsuarioId(userDetails, session);
        if (usuarioId == null) return "redirect:/login";

        String texto = comentario != null ? comentario.trim() : "";
        if (texto.isEmpty() || texto.length() > MAX_COMENTARIO_LENGTH) {
            redirectAttributes.addFlashAttribute("errorResena",
                    "El comentario debe tener entre 1 y " + MAX_COMENTARIO_LENGTH + " caracteres.");
            return "redirect:/lugares/" + lugarId;
        }
        if (calificacion == null || calificacion < 1 || calificacion > 5) {
            redirectAttributes.addFlashAttribute("errorResena", "La calificación debe estar entre 1 y 5.");
            return "redirect:/lugares/" + lugarId;
        }

        String resultado = resenaService.guardar(lugarId, usuarioId, texto, calificacion);
        if ("Reseña guardada.".equals(resultado)) {
            redirectAttributes.addFlashAttribute("mensajeResena", resultado);
        } else {
            redirectAttributes.addFlashAttribute("errorResena", resultado);
        }
        return "redirect:/lugares/" + lugarId;
    }

    @PostMapping("/resenas/{id}/editar")
    public String editarResena(@PathVariable Long id,
                               @RequestParam String comentario,
                               @RequestParam Integer calificacion,
                               @AuthenticationPrincipal CustomUserDetails userDetails,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        Long usuarioId = obtenerUsuarioId(userDetails, session);
        if (usuarioId == null) return "redirect:/login";

        Resenas resena = resenaService.obtenerPorId(id);
        if (resena == null) return "redirect:/lugares";
        Long lugarId = resena.getLugar().getId();

        String texto = comentario != null ? comentario.trim() : "";
        if (texto.isEmpty() || texto.length() > MAX_COMENTARIO_LENGTH) {
            redirectAttributes.addFlashAttribute("errorResena",
                    "El comentario debe tener entre 1 y " + MAX_COMENTARIO_LENGTH + " caracteres.");
            return "redirect:/lugares/" + lugarId;
        }
        resenaService.editar(id, usuarioId, texto, calificacion);
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