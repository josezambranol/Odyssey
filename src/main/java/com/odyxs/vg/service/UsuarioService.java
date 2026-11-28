package com.odyxs.vg.service;

import com.odyxs.vg.dto.UsuarioRegistroDto;
import com.odyxs.vg.entity.Itinerario;
import com.odyxs.vg.entity.Resenas;
import com.odyxs.vg.entity.SuscripcionLugar;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.ItinerarioRepository;
import com.odyxs.vg.repository.ResenaRepository;
import com.odyxs.vg.repository.SuscripcionLugarRepository;
import com.odyxs.vg.repository.SuscripcionPushRepository;
import com.odyxs.vg.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired(required = false)
    private ResenaRepository resenaRepository;

    @Autowired(required = false)
    private SuscripcionLugarRepository suscripcionLugarRepository;

    @Autowired(required = false)
    private SuscripcionPushRepository suscripcionPushRepository;

    @Autowired(required = false)
    private ItinerarioRepository itinerarioRepository;

    @Value("${admin.default.email:admin@odyxs.com}")
    private String defaultAdminEmail;

    @Value("${admin.default.password:admin2026}")
    private String defaultAdminPassword;

    /** Registra un usuario mediante DTO validado */
    public String registrar(UsuarioRegistroDto dto) {
        return registrar(dto.getNombre(), dto.getCorreo(), dto.getContrasena(), dto.getCountry(), dto.getFechaNacimiento());
    }

    /**
     * Registra un usuario titular validando mayoría de edad (Ley 1581 de 2012 Art. 7 y Ley 1098 de 2006)
     * y almacenando prueba electrónica de consentimiento (Decreto 1377 de 2013 Art. 8).
     */
    public String registrar(String nombre, String correo, String contrasena,
                            String country, String fechaNacimiento) {
        if (defaultAdminEmail.equalsIgnoreCase(correo.trim())) {
            return "Este correo está reservado para administración.";
        }
        if (usuarioRepository.existsByCorreo(correo.trim().toLowerCase())) {
            return "El correo ya está registrado.";
        }
        if (fechaNacimiento != null && !fechaNacimiento.isBlank()) {
            try {
                LocalDate fechaNac = LocalDate.parse(fechaNacimiento.trim());
                int edad = Period.between(fechaNac, LocalDate.now()).getYears();
                if (edad < 18) {
                    return "Debes ser mayor de 18 años para registrar una cuenta titular conforme a la Ley 1581 de 2012.";
                }
            } catch (DateTimeParseException ex) {
                return "Formato de fecha de nacimiento inválido.";
            }
        }

        Usuario u = new Usuario();
        u.setNombre(nombre.trim());
        u.setCorreo(correo.trim().toLowerCase());
        u.setContrasena(encoder.encode(contrasena));
        u.setRol(Usuario.Rol.USUARIO);
        if (country != null && !country.isBlank()) u.setCountry(country.trim());
        if (fechaNacimiento != null && !fechaNacimiento.isBlank()) u.setFechaNacimiento(fechaNacimiento.trim());
        u.setFechaConsentimiento(LocalDateTime.now());
        u.setVersionPolitica(Usuario.VERSION_POLITICA_ACTUAL);
        usuarioRepository.save(u);
        return "Registro exitoso.";
    }

    public Usuario login(String correo, String contrasena) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase())
            .filter(u -> encoder.matches(contrasena, u.getContrasena()))
            .orElse(null);
    }

    public Optional<Usuario> obtenerPorId(Long id) {
        if (id == null) return Optional.empty();
        return usuarioRepository.findById(id);
    }

    public Optional<Usuario> obtenerPorCorreo(String correo) {
        if (correo == null || correo.isBlank()) return Optional.empty();
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase());
    }

    /**
     * Derecho ARCO de Acceso y Conocimiento (Art. 8 Ley 1581 de 2012):
     * Exporta en formato estructurado la totalidad de datos personales asociados al titular.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> exportarDatosTitularArco(Long usuarioId) {
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Titular no encontrado."));

        Map<String, Object> expediente = new LinkedHashMap<>();
        expediente.put("marcoLegal", "Ley Estatutaria 1581 de 2012 y Decreto 1377 de 2013 (Colombia) - Derecho ARCO de Acceso");
        expediente.put("generadoEn", LocalDateTime.now().toString());

        Map<String, Object> perfil = new LinkedHashMap<>();
        perfil.put("id", u.getId());
        perfil.put("nombre", u.getNombre());
        perfil.put("correo", u.getCorreo());
        perfil.put("paisOrigen", u.getCountry());
        perfil.put("fechaNacimiento", u.getFechaNacimiento());
        perfil.put("rol", u.getRol().name());
        perfil.put("contrasenaProtegidaBCrypt", true);
        expediente.put("titular", perfil);

        Map<String, Object> consentimiento = new LinkedHashMap<>();
        consentimiento.put("autorizacionOtorgada", true);
        consentimiento.put("fechaConsentimiento", u.getFechaConsentimiento() != null ? u.getFechaConsentimiento().toString() : "Registro previo homologado");
        consentimiento.put("versionPolitica", u.getVersionPolitica() != null ? u.getVersionPolitica() : Usuario.VERSION_POLITICA_ACTUAL);
        expediente.put("pruebaConsentimiento", consentimiento);

        if (suscripcionLugarRepository != null) {
            List<Map<String, Object>> subs = suscripcionLugarRepository
                    .findByUsuarioIdAndActivoTrueOrderByFechaSuscripcionDesc(usuarioId)
                    .stream()
                    .map((SuscripcionLugar s) -> Map.<String, Object>of(
                            "lugarId", s.getLugarId(),
                            "nombreLugar", s.getNombreLugar() != null ? s.getNombreLugar() : "",
                            "fechaSuscripcion", s.getFechaSuscripcion() != null ? s.getFechaSuscripcion().toString() : ""
                    ))
                    .toList();
            expediente.put("suscripcionesAlertasSitios", subs);
        }

        if (resenaRepository != null) {
            List<Map<String, Object>> resenas = resenaRepository.findByUsuarioId(usuarioId)
                    .stream()
                    .map((Resenas r) -> Map.<String, Object>of(
                            "resenaId", r.getId(),
                            "lugar", r.getLugar() != null ? r.getLugar().getNombre() : "",
                            "calificacion", r.getCalificacion(),
                            "comentario", r.getComentario() != null ? r.getComentario() : "",
                            "fecha", r.getFecha() != null ? r.getFecha().toString() : ""
                    ))
                    .toList();
            expediente.put("resenasPublicadas", resenas);
        }

        if (itinerarioRepository != null) {
            List<String> itinerarios = itinerarioRepository.findByUsuarioId(usuarioId)
                    .stream()
                    .map(Itinerario::getCodigo)
                    .toList();
            expediente.put("itinerariosGenerados", itinerarios);
        }

        return expediente;
    }

    /**
     * Derecho ARCO de Supresión y Revocatoria de Autorización (Art. 8 Ley 1581 de 2012):
     * Elimina de forma transaccional todos los registros personales dependientes y la cuenta del titular.
     */
    @Transactional
    public boolean eliminarCuentaTitularArco(Long usuarioId) {
        Optional<Usuario> userOpt = usuarioRepository.findById(usuarioId);
        if (userOpt.isEmpty()) return false;
        Usuario u = userOpt.get();
        if (u.getRol() == Usuario.Rol.ADMIN) {
            return false;
        }

        if (resenaRepository != null) resenaRepository.deleteByUsuarioId(usuarioId);
        if (suscripcionLugarRepository != null) suscripcionLugarRepository.deleteByUsuarioId(usuarioId);
        if (suscripcionPushRepository != null) suscripcionPushRepository.deleteByUsuarioId(usuarioId);
        if (itinerarioRepository != null) itinerarioRepository.deleteByUsuarioId(usuarioId);

        usuarioRepository.delete(u);
        return true;
    }

    /** Inicializa el administrador al arrancar la app.
     *  Si ya existe con contraseña en texto plano, la migra a BCrypt automáticamente. */
    public void inicializarAdmin() {
        String[] adminEmails = {defaultAdminEmail, "admin@odyssey.com", "admin@odyxs.com"};
        for (String email : adminEmails) {
            if (email == null || email.isBlank()) continue;
            var adminOpt = usuarioRepository.findByCorreo(email.trim().toLowerCase());
            if (adminOpt.isEmpty()) {
                Usuario a = new Usuario();
                a.setNombre("Administrador Odyssey");
                a.setCorreo(email.trim().toLowerCase());
                a.setContrasena(encoder.encode(defaultAdminPassword));
                a.setRol(Usuario.Rol.ADMIN);
                a.setFechaConsentimiento(LocalDateTime.now());
                a.setVersionPolitica(Usuario.VERSION_POLITICA_ACTUAL);
                usuarioRepository.save(a);
            } else {
                Usuario a = adminOpt.get();
                a.setRol(Usuario.Rol.ADMIN);
                if (!a.getContrasena().startsWith("$2")) {
                    a.setContrasena(encoder.encode(defaultAdminPassword));
                    usuarioRepository.save(a);
                }
            }
        }
    }
}
