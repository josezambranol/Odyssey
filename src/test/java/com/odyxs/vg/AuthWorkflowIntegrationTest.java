package com.odyxs.vg;

import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.CategoriaRepository;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.repository.UsuarioRepository;
import com.odyxs.vg.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthWorkflowIntegrationTest {

    @MockBean private JavaMailSender javaMailSender;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LugarRepository lugarRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    public void testRegistroYLogin() throws Exception {
        String uniqueEmail = "testuser_" + System.currentTimeMillis() + "@odyssey.test";

        mockMvc.perform(post("/registro")
                .with(csrf())
                .param("nombre", "Viajero Prueba")
                .param("correo", uniqueEmail)
                .param("contrasena", "clave123")
                .param("country", "Colombia")
                .param("fechaNacimiento", "1998-05-15")
                .param("aceptaPolitica", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        assertTrue(usuarioRepository.existsByCorreo(uniqueEmail));

        mockMvc.perform(post("/login")
                .with(csrf())
                .param("correo", uniqueEmail)
                .param("contrasena", "clave123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

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

    @Test
    @WithMockUser(username = "viajero@odyssey.com", roles = "USUARIO")
    public void testLogout() throws Exception {
        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    public void testRegistroSinAceptarPoliticaEsRechazado() throws Exception {
        String email = "sin_consentimiento_" + System.currentTimeMillis() + "@odyssey.test";
        mockMvc.perform(post("/registro")
                .with(csrf())
                .param("nombre", "Usuario Sin Consentimiento")
                .param("correo", email)
                .param("contrasena", "clave123"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attributeExists("error"));

        assertFalse(usuarioRepository.existsByCorreo(email));
    }

    @Test
    public void testPaginasLegalesYProteccionAdminApi() throws Exception {
        mockMvc.perform(get("/terminos")).andExpect(status().isOk());
        mockMvc.perform(get("/privacidad")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/reportes/benchmark/seed-15k"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/idioma?lang=en").header("Referer", "https://evil.example.com/phish"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    @WithMockUser(username = "viajero@odyssey.com", roles = "USUARIO")
    public void testUsuarioNormalBloqueadoEnConfigKeyYCategorias() throws Exception {
        mockMvc.perform(post("/chatbot/config-key")
                .with(csrf())
                .contentType("application/json")
                .content("{\"apiKey\":\"TEST\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/categorias")
                .with(csrf())
                .param("nombre", "NO_AUTORIZADA"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/reportes"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void testEventosSincronizarYLugarPendienteNoExpuesto() throws Exception {
        mockMvc.perform(get("/eventos/sincronizar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/eventos?synced=true"));

        mockMvc.perform(get("/api/v1/reportes"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/api/v1/itinerarios/usuario/1"))
                .andExpect(status().is3xxRedirection());

        Categoria cat = categoriaRepository.findAll().stream().findFirst().orElseGet(() -> {
            Categoria c = new Categoria();
            c.setNombre("TEST_CAT_" + System.currentTimeMillis());
            return categoriaRepository.save(c);
        });
        Lugar pendiente = new Lugar();
        pendiente.setNombre("Sitio Pendiente Privado");
        pendiente.setUbicacion("Cartagena");
        pendiente.setCategoria(cat);
        pendiente.setEstado(Lugar.Estado.PENDIENTE);
        pendiente.setEsOficial(false);
        pendiente = lugarRepository.save(pendiente);

        mockMvc.perform(get("/lugares/" + pendiente.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/lugares"));

        mockMvc.perform(get("/api/v1/lugares/" + pendiente.getId()))
                .andExpect(status().isNotFound());
    }
}
