package com.odyxs.vg;

import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OdysseyLegalComplianceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void ley1581YDecreto1377_PersistePruebaConsentimientoYPermiteDerechosArcoAccesoYSupresion() throws Exception {
        String email = "arco_titular_" + System.currentTimeMillis() + "@odyssey.test";

        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Titular Habeas Data")
                        .param("correo", email)
                        .param("contrasena", "segura2026")
                        .param("country", "Colombia")
                        .param("fechaNacimiento", "1996-04-20")
                        .param("aceptaPolitica", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        Usuario guardado = usuarioRepository.findByCorreo(email).orElseThrow();
        assertNotNull(guardado.getFechaConsentimiento(), "Debe guardar el timestamp probatorio del consentimiento");
        assertEquals(Usuario.VERSION_POLITICA_ACTUAL, guardado.getVersionPolitica());

        MockHttpSession session = new MockHttpSession();
        MvcResult loginRes = mockMvc.perform(post("/login")
                        .session(session)
                        .with(csrf())
                        .param("correo", email)
                        .param("contrasena", "segura2026"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession authSession = (MockHttpSession) loginRes.getRequest().getSession(false);
        assertNotNull(authSession);

        // Derecho ARCO de Acceso y Portabilidad JSON
        mockMvc.perform(get("/menu/mis-datos-arco").session(authSession))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("mis-datos-odyssey-ley1581.json")))
                .andExpect(jsonPath("$.titular.correo").value(email))
                .andExpect(jsonPath("$.pruebaConsentimiento.autorizacionOtorgada").value(true))
                .andExpect(jsonPath("$.pruebaConsentimiento.versionPolitica").value(Usuario.VERSION_POLITICA_ACTUAL));

        // Derecho ARCO de Supresión y Revocatoria
        mockMvc.perform(post("/menu/eliminar-cuenta")
                        .session(authSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?deleted=true"));

        assertFalse(usuarioRepository.existsByCorreo(email), "La cuenta del titular debe eliminarse de la base de datos");
    }

    @Test
    void ley1581Art7YLey1098_RechazaRegistroDeMenoresDe18Anos() throws Exception {
        String emailMenor = "menor_" + System.currentTimeMillis() + "@odyssey.test";

        mockMvc.perform(post("/registro")
                        .with(csrf())
                        .param("nombre", "Adolescente Prueba")
                        .param("correo", emailMenor)
                        .param("contrasena", "segura2026")
                        .param("country", "Colombia")
                        .param("fechaNacimiento", "2012-08-10")
                        .param("aceptaPolitica", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("registro"))
                .andExpect(model().attribute("error", org.hamcrest.Matchers.containsString("mayor de 18 años")));

        assertFalse(usuarioRepository.existsByCorreo(emailMenor));
    }

    @Test
    void ley679EscnnaYLey2068Turismo_TerminosYPrivacidadIncluyenClausulasObligatorias() throws Exception {
        mockMvc.perform(get("/terminos"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ley 679 de 2001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ESCNNA")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("MIT License")));

        mockMvc.perform(get("/privacidad"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Web Push PWA")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SHA-256")));
    }
}
