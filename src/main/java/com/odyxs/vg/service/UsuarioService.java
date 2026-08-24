package com.odyxs.vg.service;

import com.odyxs.vg.dto.UsuarioRegistroDto;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Value("${admin.default.email:admin@odyxs.com}")
    private String defaultAdminEmail;

    @Value("${admin.default.password:admin2026}")
    private String defaultAdminPassword;

    /** Registra un usuario mediante DTO validado */
    public String registrar(UsuarioRegistroDto dto) {
        return registrar(dto.getNombre(), dto.getCorreo(), dto.getContrasena(), dto.getCountry(), dto.getFechaNacimiento());
    }

    /** Registra un usuario normal. */
    public String registrar(String nombre, String correo, String contrasena,
                            String country, String fechaNacimiento) {
        if (defaultAdminEmail.equalsIgnoreCase(correo.trim())) {
            return "Este correo está reservado para administración.";
        }
        if (usuarioRepository.existsByCorreo(correo.trim())) {
            return "El correo ya está registrado.";
        }
        Usuario u = new Usuario();
        u.setNombre(nombre.trim());
        u.setCorreo(correo.trim().toLowerCase());
        u.setContrasena(encoder.encode(contrasena));
        u.setRol(Usuario.Rol.USUARIO);
        if (country != null && !country.isBlank()) u.setCountry(country.trim());
        if (fechaNacimiento != null && !fechaNacimiento.isBlank()) u.setFechaNacimiento(fechaNacimiento.trim());
        usuarioRepository.save(u);
        return "Registro exitoso.";
    }

    public Usuario login(String correo, String contrasena) {
        return usuarioRepository.findByCorreo(correo.trim().toLowerCase())
            .filter(u -> encoder.matches(contrasena, u.getContrasena()))
            .orElse(null);
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
