package com.odyxs.vg.controller;

import com.odyxs.vg.dto.UsuarioRegistroDto;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.service.UsuarioService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

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
                               Model model) {
        if (error != null) {
            model.addAttribute("error", "Correo o contraseña incorrectos.");
        }
        if (logout != null) {
            model.addAttribute("mensaje", "Has cerrado sesión correctamente.");
        }
        if (registered != null) {
            model.addAttribute("mensaje", "¡Registro exitoso! Por favor inicia sesión con tu cuenta.");
        }
        return "login";
    }

    @GetMapping("/logout")
    public String logout(jakarta.servlet.http.HttpServletRequest request,
                         jakarta.servlet.http.HttpServletResponse response) {
        org.springframework.security.core.Authentication auth =
            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            new org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler().logout(request, response, auth);
        }
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model, Principal principal) {
        Object nombre = session.getAttribute("usuarioNombre");
        if (nombre == null && principal != null) {
            nombre = principal.getName();
        }
        model.addAttribute("nombre", nombre != null ? nombre : "Usuario");
        return "menu";
    }
}
