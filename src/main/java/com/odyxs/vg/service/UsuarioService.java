package com.odyxs.vg.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.UsuarioRepository;

@Service
public class UsuarioService {

    @Autowired private UsuarioRepository usuarioRepository;

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** Registra un usuario normal. El correo admin@odyxs.com está reservado. */
    public String registrar(String nombre, String correo, String contrasena,
                            String country, String fechaNacimiento) {
        if ("admin@odyxs.com".equalsIgnoreCase(correo))
            return "Este correo está reservado.";
        if (usuarioRepository.existsByCorreo(correo))
            return "El correo ya está registrado.";
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo(correo);
        u.setContrasena(encoder.encode(contrasena));
        u.setRol(Usuario.Rol.USUARIO);
        if (country != null && !country.isBlank()) u.setCountry(country);
        if (fechaNacimiento != null && !fechaNacimiento.isBlank()) u.setFechaNacimiento(fechaNacimiento);
        usuarioRepository.save(u);
        return "Registro exitoso.";
    }

    public Usuario login(String correo, String contrasena) {
        return usuarioRepository.findByCorreo(correo)
            .filter(u -> encoder.matches(contrasena, u.getContrasena()))
            .orElse(null);
    }

    /** Inicializa el administrador único al arrancar la app.
     *  Si ya existe con contraseña en texto plano, la migra a BCrypt. */
    public void inicializarAdmin() {
        var adminOpt = usuarioRepository.findByCorreo("admin@odyxs.com");
        if (adminOpt.isEmpty()) {
            Usuario a = new Usuario();
            a.setNombre("Administrador ODYXS");
            a.setCorreo("admin@odyxs.com");
            a.setContrasena(encoder.encode("admin2026"));
            a.setRol(Usuario.Rol.ADMIN);
            usuarioRepository.save(a);
        } else {
            // Migrar contraseña a BCrypt si todavía está en texto plano
            Usuario a = adminOpt.get();
            if (!a.getContrasena().startsWith("$2")) {
                a.setContrasena(encoder.encode(a.getContrasena()));
                usuarioRepository.save(a);
            }
        }
    }
}
