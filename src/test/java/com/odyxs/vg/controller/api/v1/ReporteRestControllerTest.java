package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.domain.mongo.Reporte;
import com.odyxs.vg.security.CustomAuthenticationSuccessHandler;
import com.odyxs.vg.security.CustomUserDetailsService;
import com.odyxs.vg.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ReporteRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReporteRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReporteService reporteService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private CustomAuthenticationSuccessHandler successHandler;

    @Test
    @WithMockUser
    void listar_RetornaPaginaConExito() throws Exception {
        Reporte reporte = new Reporte();
        reporte.setId("rep1");
        reporte.setCodigo("REP-001");
        reporte.setTitulo("Visita a Castillo San Felipe");
        reporte.setTipo(Reporte.TipoReporte.VISITA_TURISTICA);

        when(reporteService.listarPaginado(eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reporte)));

        mockMvc.perform(get("/api/v1/reportes")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].codigo").value("REP-001"))
                .andExpect(jsonPath("$.data.content[0].titulo").value("Visita a Castillo San Felipe"));
    }

    @Test
    @WithMockUser
    void obtenerMetricas_RetornaAgregaciones() throws Exception {
        Map<String, Object> stats = Map.of(
                "totalReportes", 15420L,
                "conteoPorTipo", Map.of("VISITA_TURISTICA", 8000L)
        );
        when(reporteService.obtenerEstadisticasGlobales()).thenReturn(stats);

        mockMvc.perform(get("/api/v1/reportes/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalReportes").value(15420));
    }

    @Test
    @WithMockUser
    void benchmark15k_EjecutaConExito() throws Exception {
        Map<String, Object> benchResult = Map.of(
                "status", "SUCCESS",
                "documentosGeneradosEnEstaPrueba", 15000,
                "tiempoTotalInsercionMs", 1120
        );
        when(reporteService.ejecutarBenchmark15000Documentos()).thenReturn(benchResult);

        mockMvc.perform(post("/api/v1/reportes/benchmark/seed-15k"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.documentosGeneradosEnEstaPrueba").value(15000));
    }

    @Test
    @WithMockUser
    void obtenerPorId_NoEncontrado_Retorna404() throws Exception {
        when(reporteService.buscarPorId("no_existe")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/reportes/no_existe"))
                .andExpect(status().isNotFound());
    }
}
