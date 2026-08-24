package com.odyxs.vg.service;

import com.odyxs.vg.dto.UsuarioRegistroDto;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(usuarioService, "defaultAdminEmail", "admin@odyssey.com");
        ReflectionTestUtils.setField(usuarioService, "defaultAdminPassword", "admin2026");
    }

    @Test
    void registrar_Exito() {
        UsuarioRegistroDto dto = new UsuarioRegistroDto("Juan Perez", "juan@test.com", "12345", "Colombia", "1995-01-01");
        when(usuarioRepository.existsByCorreo("juan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("12345")).thenReturn("$2a$10$encodedPassword");

        String resultado = usuarioService.registrar(dto);

        assertEquals("Registro exitoso.", resultado);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    void registrar_CorreoDuplicado_Falla() {
        UsuarioRegistroDto dto = new UsuarioRegistroDto("Juan Perez", "juan@test.com", "12345", "Colombia", "1995-01-01");
        when(usuarioRepository.existsByCorreo("juan@test.com")).thenReturn(true);

        String resultado = usuarioService.registrar(dto);

        assertEquals("El correo ya está registrado.", resultado);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void login_Exitoso() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("juan@test.com");
        usuario.setContrasena("$2a$10$encodedPassword");

        when(usuarioRepository.findByCorreo("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("12345", "$2a$10$encodedPassword")).thenReturn(true);

        Usuario res = usuarioService.login("juan@test.com", "12345");

        assertNotNull(res);
        assertEquals("juan@test.com", res.getCorreo());
    }

    @Test
    void login_ContrasenaIncorrecta_RetornaNull() {
        Usuario usuario = new Usuario();
        usuario.setCorreo("juan@test.com");
        usuario.setContrasena("$2a$10$encodedPassword");

        when(usuarioRepository.findByCorreo("juan@test.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("wrongpass", "$2a$10$encodedPassword")).thenReturn(false);

        Usuario res = usuarioService.login("juan@test.com", "wrongpass");

        assertNull(res);
    }
}