package com.odyxs.vg;

import com.odyxs.vg.dto.UsuarioRegistroDto;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.UsuarioRepository;
import com.odyxs.vg.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    public void testRegistroYLogin() throws Exception {
        String uniqueEmail = "testuser_" + System.currentTimeMillis() + "@odyssey.test";

        // 1. Probar Registro con CSRF
        mockMvc.perform(post("/registro")
                .with(csrf())
                .param("nombre", "Viajero Prueba")
                .param("correo", uniqueEmail)
                .param("contrasena", "clave123")
                .param("country", "Colombia")
                .param("fechaNacimiento", "1998-05-15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        // 2. Verificar que el usuario se guardó en BD
        assertTrue(usuarioRepository.existsByCorreo(uniqueEmail));

        // 3. Probar Login con credenciales válidas
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("correo", uniqueEmail)
                .param("contrasena", "clave123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 4. Probar Login con contraseña incorrecta
        mockMvc.perform(post("/login")
                .with(csrf())
                .param("correo", uniqueEmail)
                .param("contrasena", "clave_erronea"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    public void testAdminLogin() throws Exception {
        usuarioService.inicializarAdmin();

        mockMvc.perform(post("/login")
                .with(csrf())
                .param("correo", "admin@odyssey.com")
                .param("contrasena", "admin2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }
}