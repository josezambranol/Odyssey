package com.odyxs.vg.security;

import com.odyxs.vg.entity.Usuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        if (authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            Usuario u = userDetails.getUsuario();
            HttpSession session = request.getSession();
            session.setAttribute("usuarioId", u.getId());
            session.setAttribute("usuarioCorreo", u.getCorreo());
            session.setAttribute("usuarioNombre", u.getNombre());
            session.setAttribute("usuarioRol", u.getRol().name());

            if (u.getRol() == Usuario.Rol.ADMIN) {
                response.sendRedirect("/admin");
                return;
            }
        }
        response.sendRedirect("/");
    }
}