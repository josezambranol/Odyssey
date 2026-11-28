package com.odyxs.vg.controller;

import com.odyxs.vg.dto.UsuarioRegistroDto;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.service.AuthCodigoService;
import com.odyxs.vg.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired(required = false)
    private AuthCodigoService authCodigoService;

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {
        if (!model.containsAttribute("usuario")) {
            model.addAttribute("usuario", new UsuarioRegistroDto());
        }
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@Valid @ModelAttribute("usuario") UsuarioRegistroDto dto,
                                   BindingResult bindingResult,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "registro";
        }

        String resultado = usuarioService.registrar(dto);
        if ("Registro exitoso.".equals(resultado)) {
            return "redirect:/login?registered=true";
        }
        model.addAttribute("error", resultado);
        return "registro";
    }

    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String error,
                               @RequestParam(required = false) String logout,
                               @RequestParam(required = false) String registered,
                               @RequestParam(required = false) String reset,
                               @RequestParam(required = false) String deleted,
                               Model model) {
        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("mensaje", "Has cerrado sesión correctamente.");
        }
        if (registered != null) {
            model.addAttribute("mensaje", "¡Registro exitoso! Inicia sesión con tu contraseña o mediante un código al correo.");
        }
        if (reset != null) {
            model.addAttribute("mensaje", "¡Contraseña restablecida con éxito! Ya puedes ingresar.");
        }
        if (deleted != null) {
            model.addAttribute("mensaje", "Tu autorización fue revocada y tu cuenta y datos personales fueron suprimidos conforme a la Ley 1581 de 2012.");
        }
        return "login";
    }

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model, Principal principal) {
        Optional<Usuario> userOpt = resolverUsuarioAutenticado(session, principal);
        Object nombre = session.getAttribute("usuarioNombre");
        if (nombre == null && userOpt.isPresent()) {
            nombre = userOpt.get().getNombre();
        } else if (nombre == null && principal != null) {
            nombre = principal.getName();
        }

        if (userOpt.isPresent() && authCodigoService != null) {
            Long uid = userOpt.get().getId();
            model.addAttribute("suscripciones", authCodigoService.obtenerSuscripcionesUsuario(uid));
            model.addAttribute("usuarioActual", userOpt.get());
        } else {
            model.addAttribute("suscripciones", List.of());
        }
        model.addAttribute("nombre", nombre != null ? nombre : "Viajero");
        return "menu";
    }

    /**
     * Derecho ARCO de Acceso / Conocimiento (Ley 1581 de 2012, Art. 8):
     * Descarga el expediente completo de datos personales del titular en formato JSON.
     */
    @GetMapping("/menu/mis-datos-arco")
    public ResponseEntity<Map<String, Object>> descargarMisDatosArco(HttpSession session, Principal principal) {
        Optional<Usuario> userOpt = resolverUsuarioAutenticado(session, principal);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Object> expediente = usuarioService.exportarDatosTitularArco(userOpt.get().getId());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mis-datos-odyssey-ley1581.json\"")
                .body(expediente);
    }

    /**
     * Derecho ARCO de Supresión y Revocatoria (Ley 1581 de 2012, Art. 8):
     * Elimina permanentemente la cuenta y datos personales asociados al titular.
     */
    @PostMapping("/menu/eliminar-cuenta")
    public String eliminarMiCuentaArco(HttpServletRequest request,
                                       HttpSession session,
                                       Principal principal,
                                       RedirectAttributes redirectAttributes) {
        Optional<Usuario> userOpt = resolverUsuarioAutenticado(session, principal);
        if (userOpt.isEmpty()) {
            return "redirect:/login";
        }
        if (userOpt.get().getRol() == Usuario.Rol.ADMIN) {
            redirectAttributes.addFlashAttribute("errorArco", "La cuenta institucional de administración no puede auto-eliminarse desde esta vista.");
            return "redirect:/menu";
        }

        boolean eliminado = usuarioService.eliminarCuentaTitularArco(userOpt.get().getId());
        if (eliminado) {
            SecurityContextHolder.clearContext();
            HttpSession currentSession = request.getSession(false);
            if (currentSession != null) {
                currentSession.invalidate();
            }
            return "redirect:/login?deleted=true";
        }
        redirectAttributes.addFlashAttribute("errorArco", "No fue posible procesar la supresión en este momento.");
        return "redirect:/menu";
    }

    private Optional<Usuario> resolverUsuarioAutenticado(HttpSession session, Principal principal) {
        if (session != null && session.getAttribute("usuarioId") != null) {
            Long uid = Long.valueOf(session.getAttribute("usuarioId").toString());
            Optional<Usuario> porId = usuarioService.obtenerPorId(uid);
            if (porId.isPresent()) return porId;
        }
        if (principal != null && principal.getName() != null) {
            return usuarioService.obtenerPorCorreo(principal.getName());
        }
        return Optional.empty();
    }
}
