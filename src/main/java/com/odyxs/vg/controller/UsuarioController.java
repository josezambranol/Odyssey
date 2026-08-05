package com.odyxs.vg.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.service.UsuarioService;

import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String nombre,
                                   @RequestParam String correo,
                                   @RequestParam String contrasena,
                                   @RequestParam(required = false) String country,
                                   @RequestParam(required = false) String fechaNacimiento,
                                   Model model) {
        String resultado = usuarioService.registrar(nombre, correo, contrasena, country, fechaNacimiento);
        if (resultado.equals("Registro exitoso.")) {
            return "redirect:/login";
        }
        model.addAttribute("error", resultado);
        return "registro";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String correo,
                                @RequestParam String contrasena,
                                Model model,
                                HttpSession session) {
        Usuario usuario = usuarioService.login(correo, contrasena);
        if (usuario != null) {
            session.setAttribute("usuarioId", usuario.getId());
            session.setAttribute("usuarioCorreo", usuario.getCorreo());
            session.setAttribute("usuarioNombre", usuario.getNombre());
            session.setAttribute("usuarioRol", usuario.getRol().name());
            return "redirect:/";
        }
        model.addAttribute("error", "Correo o contraseña incorrectos.");
        return "login";
    }

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) {
            return "redirect:/login";
        }
        model.addAttribute("nombre", session.getAttribute("usuarioNombre"));
        return "menu";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
