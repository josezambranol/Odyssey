package com.odyxs.vg.controller;

import com.odyxs.vg.domain.mongo.CodigoVerificacion;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.security.CustomUserDetails;
import com.odyxs.vg.service.AuthCodigoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Controller
public class AuthCodigoController {

    @Autowired
    private AuthCodigoService authCodigoService;

    @GetMapping("/recuperar-password")
    public String mostrarRecuperarPassword() {
        return "recuperar-password";
    }

    /**
     * Solicita el envío de un código de 6 dígitos al correo (para Login por Código o Recuperar Contraseña).
     */
    @PostMapping("/auth/codigo/solicitar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> solicitarCodigo(@RequestBody Map<String, String> payload) {
        String correo = payload.getOrDefault("correo", "");
        String tipoStr = payload.getOrDefault("tipo", "LOGIN_OTP");

        CodigoVerificacion.TipoCodigo tipo = "RECUPERAR_PASSWORD".equalsIgnoreCase(tipoStr)
                ? CodigoVerificacion.TipoCodigo.RECUPERAR_PASSWORD
                : CodigoVerificacion.TipoCodigo.LOGIN_OTP;

        Map<String, Object> resultado = authCodigoService.solicitarCodigo(correo, tipo);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Verifica el código OTP de 6 dígitos e inicia la sesión de Spring Security automáticamente.
     */
    @PostMapping("/auth/codigo/login")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> loginConCodigo(@RequestBody Map<String, String> payload,
                                                              HttpServletRequest request) {
        String correo = payload.getOrDefault("correo", "");
        String codigo = payload.getOrDefault("codigo", "");

        Optional<Usuario> userOpt = authCodigoService.validarCodigoYObtenerUsuario(correo, codigo);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "exito", false,
                    "mensaje", "El código de 6 dígitos es inválido o ha expirado. Verifica tu correo e inténtalo de nuevo."
            ));
        }

        Usuario u = userOpt.get();
        CustomUserDetails userDetails = new CustomUserDetails(u);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext sc = SecurityContextHolder.createEmptyContext();
        sc.setAuthentication(authToken);
        SecurityContextHolder.setContext(sc);

        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, sc);
        session.setAttribute("usuarioId", u.getId());
        session.setAttribute("usuarioCorreo", u.getCorreo());
        session.setAttribute("usuarioNombre", u.getNombre());
        session.setAttribute("usuarioRol", u.getRol().name());

        String redirectUrl = (u.getRol() == Usuario.Rol.ADMIN) ? "/admin" : "/";
        return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", "¡Bienvenido de vuelta, " + u.getNombre() + "!",
                "redirectUrl", redirectUrl
        ));
    }

    /**
     * Restablece la contraseña usando el código de 6 dígitos enviado al correo.
     */
    @PostMapping("/auth/codigo/restablecer-password")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restablecerPassword(@RequestBody Map<String, String> payload) {
        String correo = payload.getOrDefault("correo", "");
        String codigo = payload.getOrDefault("codigo", "");
        String nuevaPassword = payload.getOrDefault("nuevaPassword", "");

        Map<String, Object> resultado = authCodigoService.restablecerPasswordConCodigo(correo, codigo, nuevaPassword);
        return ResponseEntity.ok(resultado);
    }

    /**
     * Activa o desactiva la suscripción a novedades de un sitio turístico de interés.
     */
    @PostMapping("/suscripciones/lugar/{lugarId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> alternarSuscripcion(@PathVariable Long lugarId,
                                                                   HttpSession session) {
        Object usuarioIdObj = session.getAttribute("usuarioId");
        if (usuarioIdObj == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "exito", false,
                    "requiereLogin", true,
                    "mensaje", "Inicia sesión para recibir novedades y alertas de este sitio en tu correo."
            ));
        }
        Long usuarioId = Long.valueOf(usuarioIdObj.toString());
        Map<String, Object> resp = authCodigoService.alternarSuscripcionLugar(usuarioId, lugarId);
        return ResponseEntity.ok(resp);
    }
}
