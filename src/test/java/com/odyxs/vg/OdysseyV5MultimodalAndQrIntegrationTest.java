package com.odyxs.vg;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OdysseyV5MultimodalAndQrIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void vistaEscanerYTarifasReferenciaV5_CarganCorrectamente() throws Exception {
        mockMvc.perform(get("/escaner"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Apunta, Descubre y Verifica Precios")));

        mockMvc.perform(get("/api/v1/vision/tarifas-referencia"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(9));
    }

    @Test
    void escudoAntiSobrecobrosV5_DistingueCobroAbusivoDePrecioJusto() throws Exception {
        mockMvc.perform(multipart("/api/v1/vision/analizar")
                        .param("modo", "PRECIOS")
                        .param("descripcion", "Mojarra frita 220.000, cerveza nacional 42.000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nivelAlerta").value("ALERTA_SOBRECOBRO"))
                .andExpect(jsonPath("$.data.montoDetectadoCop").isNumber());

        mockMvc.perform(multipart("/api/v1/vision/analizar")
                        .param("modo", "PRECIOS")
                        .param("descripcion", "Mojarra frita 65.000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nivelAlerta").value("PRECIO_JUSTO"));
    }

    @Test
    void escanerPatrimonioMultimodalV5_ProcesaImagenEnMemoriaYRechazaMimeInvalido() throws Exception {
        byte[] fakePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile fotoValida = new MockMultipartFile(
                "imagen", "castillo-san-felipe.png", "image/png", fakePng);

        MvcResult okRes = mockMvc.perform(multipart("/api/v1/vision/analizar")
                        .file(fotoValida)
                        .param("modo", "PATRIMONIO")
                        .param("descripcion", "Castillo San Felipe tuneles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.nivelAlerta").value("PATRIMONIO_IDENTIFICADO"))
                .andReturn();
        assertTrue(okRes.getResponse().getContentAsString(StandardCharsets.UTF_8).contains("San Felipe"));

        MockMultipartFile ejecutableMalicioso = new MockMultipartFile(
                "imagen", "script.exe", "application/octet-stream", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/v1/vision/analizar")
                        .file(ejecutableMalicioso)
                        .param("modo", "PRECIOS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void pasesQrFirmadosHmacSha256YRntV5_VerificaPaseAutenticoYDetectaFalsificacion() throws Exception {
        String reqBody = """
                {
                  "nombreViajero": "Jose Zambrano",
                  "destinoActividad": "Pasadía Islas del Rosario",
                  "personas": 2,
                  "precioAcordadoCop": 120000,
                  "numeroRnt": "48291"
                }
                """;

        MvcResult emitRes = mockMvc.perform(post("/api/v1/pases/emitir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rntFormatoValido").value(true))
                .andExpect(jsonPath("$.data.svgVisualQr").value(org.hamcrest.Matchers.containsString("<svg")))
                .andReturn();

        String json = emitRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String marker = "\"tokenFirmado\":\"";
        int start = json.indexOf(marker) + marker.length();
        int end = json.indexOf('"', start);
        String tokenOriginal = json.substring(start, end);

        // 1. Verificación legítima pasa
        mockMvc.perform(get("/api/v1/pases/verificar").param("token", tokenOriginal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autentico").value(true))
                .andExpect(jsonPath("$.data.vigente").value(true))
                .andExpect(jsonPath("$.data.datosDecodificados.rnt").value("48291"));

        // 2. Alterar 1 solo carácter de la firma dispara detección criptográfica de fraude
        char ultimo = tokenOriginal.charAt(tokenOriginal.length() - 1);
        String tokenAdulterado = tokenOriginal.substring(0, tokenOriginal.length() - 1) + (ultimo == 'A' ? 'B' : 'A');

        mockMvc.perform(get("/api/v1/pases/verificar").param("token", tokenAdulterado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.autentico").value(false))
                .andExpect(jsonPath("$.data.mensajeVeredicto").value(org.hamcrest.Matchers.containsString("HMAC-SHA256 inválida")));
    }
}
