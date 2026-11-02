package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.CodigoVerificacion;
import com.odyxs.vg.domain.mongo.SuscripcionLugar;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.repository.UsuarioRepository;
import com.odyxs.vg.repository.mongo.CodigoVerificacionRepository;
import com.odyxs.vg.repository.mongo.SuscripcionLugarRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AuthCodigoService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired(required = false)
    private CodigoVerificacionRepository codigoRepository;

    @Autowired(required = false)
    private SuscripcionLugarRepository suscripcionRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private EmailNotificationService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Respaldo en memoria en caso de que MongoDB no esté activo en un entorno aislado
    private final Map<String, CodigoVerificacion> fallbackCodigos = new HashMap<>();

    /**
     * Genera y envía un código de 6 dígitos al correo asociado del usuario.
     */
    public Map<String, Object> solicitarCodigo(String correoRaw, CodigoVerificacion.TipoCodigo tipo) {
        Map<String, Object> resp = new LinkedHashMap<>();
        if (correoRaw == null || correoRaw.isBlank()) {
            resp.put("exito", false);
            resp.put("mensaje", "Por favor ingresa un correo electrónico válido.");
            return resp;
        }

        String correo = correoRaw.trim().toLowerCase();
        Optional<Usuario> userOpt = usuarioRepository.findByCorreo(correo);
        Usuario usuario;
        if (userOpt.isEmpty()) {
            // Auto-aprovisionamiento de perfil viajero para que cualquier Gmail/correo pueda ingresar con OTP
            usuario = new Usuario();
            String alias = correo.contains("@") ? correo.substring(0, correo.indexOf('@')) : "Viajero";
            alias = alias.replace('.', ' ').replace('_', ' ');
            if (!alias.isEmpty()) {
                alias = Character.toUpperCase(alias.charAt(0)) + alias.substring(1);
            }
            usuario.setNombre(alias);
            usuario.setCorreo(correo);
            usuario.setContrasena(passwordEncoder.encode(UUID.randomUUID().toString()));
            usuario.setCountry("Colombia");
            usuario.setRol(Usuario.Rol.USUARIO);
            usuario = usuarioRepository.save(usuario);
        } else {
            usuario = userOpt.get();
        }
        String codigo = String.format("%06d", RANDOM.nextInt(900000) + 100000);
        Instant expiraEn = Instant.now().plus(10, ChronoUnit.MINUTES);

        CodigoVerificacion doc = new CodigoVerificacion(correo, codigo, tipo, expiraEn);
        String proposito = (tipo == CodigoVerificacion.TipoCodigo.RECUPERAR_PASSWORD)
                ? "Recuperación de contraseña en ODYSSEY"
                : "Inicio de sesión seguro con código único";

        boolean enviadoSmtp = emailService.enviarCodigoOtp(correo, usuario.getNombre(), codigo, proposito);
        doc.setEnviadoPorSmtp(enviadoSmtp);

        if (codigoRepository != null) {
            try {
                codigoRepository.save(doc);
            } catch (Exception ignored) {}
        }
        fallbackCodigos.put(correo, doc);

        resp.put("exito", true);
        resp.put("correo", correo);
        resp.put("enviadoSmtp", enviadoSmtp);
        if (enviadoSmtp) {
            resp.put("mensaje", "Hemos enviado un código de 6 dígitos a tu correo " + ocultarCorreo(correo) + ".");
        } else {
            resp.put("mensaje", "Código generado para " + correo + ". (Modo Demo Local: configura MAIL_USERNAME y MAIL_PASSWORD en application-local.properties para despacho Gmail directo).");
            resp.put("codigoDemo", codigo);
        }
        return resp;
    }

    /**
     * Verifica el código de 6 dígitos y retorna el Usuario si es válido.
     */
    public Optional<Usuario> validarCodigoYObtenerUsuario(String correoRaw, String codigoRaw) {
        if (correoRaw == null || codigoRaw == null) return Optional.empty();
        String correo = correoRaw.trim().toLowerCase();
        String codigo = codigoRaw.trim();

        CodigoVerificacion encontrado = null;
        if (codigoRepository != null) {
            try {
                encontrado = codigoRepository
                        .findFirstByCorreoIgnoreCaseAndCodigoAndUsadoFalseOrderByFechaCreacionDesc(correo, codigo)
                        .orElse(null);
            } catch (Exception ignored) {}
        }
        if (encontrado == null) {
            CodigoVerificacion mem = fallbackCodigos.get(correo);
            if (mem != null && !mem.isUsado() && codigo.equals(mem.getCodigo())) {
                encontrado = mem;
            }
        }

        if (encontrado == null || encontrado.estaExpirado()) {
            return Optional.empty();
        }

        encontrado.setUsado(true);
        if (codigoRepository != null && encontrado.getId() != null) {
            try {
                codigoRepository.save(encontrado);
            } catch (Exception ignored) {}
        }

        return usuarioRepository.findByCorreo(correo);
    }

    /**
     * Restablece la contraseña del usuario tras validar su código de 6 dígitos.
     */
    public Map<String, Object> restablecerPasswordConCodigo(String correoRaw, String codigoRaw, String nuevaPassword) {
        Map<String, Object> resp = new LinkedHashMap<>();
        if (nuevaPassword == null || nuevaPassword.trim().length() < 6) {
            resp.put("exito", false);
            resp.put("mensaje", "La nueva contraseña debe tener al menos 6 caracteres.");
            return resp;
        }

        Optional<Usuario> userOpt = validarCodigoYObtenerUsuario(correoRaw, codigoRaw);
        if (userOpt.isEmpty()) {
            resp.put("exito", false);
            resp.put("mensaje", "El código ingresado es inválido o ha expirado. Solicita uno nuevo.");
            return resp;
        }

        Usuario usuario = userOpt.get();
        usuario.setContrasena(passwordEncoder.encode(nuevaPassword.trim()));
        usuarioRepository.save(usuario);

        resp.put("exito", true);
        resp.put("mensaje", "¡Tu contraseña ha sido actualizada con éxito! Ya puedes iniciar sesión.");
        return resp;
    }

    /**
     * Activa o desactiva la suscripción a novedades de un sitio turístico de interés.
     */
    public Map<String, Object> alternarSuscripcionLugar(Long usuarioId, Long lugarId) {
        Map<String, Object> resp = new LinkedHashMap<>();
        if (usuarioId == null || lugarId == null) {
            resp.put("exito", false);
            resp.put("mensaje", "Debes iniciar sesión para recibir novedades de tus sitios de interés.");
            return resp;
        }

        Optional<Usuario> userOpt = usuarioRepository.findById(usuarioId);
        Optional<Lugar> lugarOpt = lugarRepository.findById(lugarId);
        if (userOpt.isEmpty() || lugarOpt.isEmpty()) {
            resp.put("exito", false);
            resp.put("mensaje", "No se encontró el lugar o el usuario.");
            return resp;
        }

        Usuario usuario = userOpt.get();
        Lugar lugar = lugarOpt.get();

        if (suscripcionRepository == null) {
            resp.put("exito", false);
            resp.put("mensaje", "Servicio de suscripciones temporalmente no disponible.");
            return resp;
        }

        Optional<SuscripcionLugar> existenteOpt = suscripcionRepository.findByUsuarioIdAndLugarId(usuarioId, lugarId);
        boolean ahoraSuscrito;

        if (existenteOpt.isPresent()) {
            SuscripcionLugar sub = existenteOpt.get();
            sub.setActivo(!sub.isActivo());
            sub.setFechaSuscripcion(Instant.now());
            suscripcionRepository.save(sub);
            ahoraSuscrito = sub.isActivo();
        } else {
            SuscripcionLugar nueva = new SuscripcionLugar();
            nueva.setUsuarioId(usuario.getId());
            nueva.setCorreo(usuario.getCorreo());
            nueva.setNombreUsuario(usuario.getNombre());
            nueva.setLugarId(lugar.getId());
            nueva.setNombreLugar(lugar.getNombre());
            nueva.setCategoriaLugar(lugar.getCategoria() != null ? lugar.getCategoria().getNombre() : "DESTINO");
            nueva.setUbicacionLugar(lugar.getUbicacion());
            nueva.setImagenLugar(lugar.getImagenEfectiva());
            nueva.setActivo(true);
            suscripcionRepository.save(nueva);
            ahoraSuscrito = true;
        }

        if (ahoraSuscrito) {
            emailService.enviarConfirmacionSuscripcion(
                    usuario.getCorreo(), usuario.getNombre(), lugar.getNombre(), lugar.getUbicacion());
            resp.put("mensaje", "🔔 ¡Suscripción activada! Recibirás novedades y eventos de " + lugar.getNombre() + " en " + usuario.getCorreo());
        } else {
            resp.put("mensaje", "Has desactivado las notificaciones para " + lugar.getNombre() + ".");
        }

        resp.put("exito", true);
        resp.put("suscrito", ahoraSuscrito);
        resp.put("totalSuscriptores", suscripcionRepository.countByLugarIdAndActivoTrue(lugarId));
        return resp;
    }

    public boolean estaSuscrito(Long usuarioId, Long lugarId) {
        if (usuarioId == null || lugarId == null || suscripcionRepository == null) return false;
        try {
            return suscripcionRepository.existsByUsuarioIdAndLugarIdAndActivoTrue(usuarioId, lugarId);
        } catch (Exception e) {
            return false;
        }
    }

    public long contarSuscriptores(Long lugarId) {
        if (lugarId == null || suscripcionRepository == null) return 0L;
        try {
            return suscripcionRepository.countByLugarIdAndActivoTrue(lugarId);
        } catch (Exception e) {
            return 0L;
        }
    }

    public List<SuscripcionLugar> obtenerSuscripcionesUsuario(Long usuarioId) {
        if (usuarioId == null || suscripcionRepository == null) return List.of();
        try {
            return suscripcionRepository.findByUsuarioIdAndActivoTrueOrderByFechaSuscripcionDesc(usuarioId);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String ocultarCorreo(String correo) {
        int at = correo.indexOf('@');
        if (at <= 2) return correo;
        return correo.substring(0, 2) + "***" + correo.substring(at);
    }
}
