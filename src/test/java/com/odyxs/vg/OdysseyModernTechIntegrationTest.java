package com.odyxs.vg;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OdysseyModernTechIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pwaAssetsYActuatorHealth_RespondenConCabecerasSeguras() throws Exception {
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().exists("Permissions-Policy"));

        mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/offline.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void aiStreamSseEndpoint_EmiteTokensSobreVirtualThreads() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/v1/ai/stream")
                        .param("q", "Mejores lugares para visitar en Cartagena")
                        .accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:meta")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:done")));
    }

    @Test
    void ragVectorialSemanticoV4_EncuentraSitiosPorIntencionNatural() throws Exception {
        mockMvc.perform(get("/api/v1/lugares/semantico")
                        .param("q", "lugar especial para pedir matrimonio y celebrar aniversario frente al mar")
                        .param("topK", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].similitudCoseno").isNumber())
                .andExpect(jsonPath("$.data[0].porcentajeAfinidad").isNumber());
    }

    @Test
    void ragVectorialSemanticoV4_DistingueHistoriaMilitarDeSnorkel() throws Exception {
        MvcResult resPiratas = mockMvc.perform(get("/api/v1/lugares/semantico")
                        .param("q", "explorar tuneles militares canones e historia de piratas")
                        .param("topK", "2"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(resPiratas.getResponse().getContentAsString().contains("San Felipe"));

        MvcResult resCoral = mockMvc.perform(get("/api/v1/lugares/semantico")
                        .param("q", "hacer snorkel y buceo en arrecifes de coral")
                .param("topK", "2"))
                .andExpect(status().isOk())
                .andReturn();
        assertTrue(resCoral.getResponse().getContentAsString().contains("Rosario"));
    }
}
