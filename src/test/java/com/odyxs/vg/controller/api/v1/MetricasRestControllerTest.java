package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.repository.mongo.ChatbotLogRepository;
import com.odyxs.vg.security.CustomAuthenticationSuccessHandler;
import com.odyxs.vg.security.CustomUserDetailsService;
import com.odyxs.vg.service.RedisMetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MetricasRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class MetricasRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisMetricsService redisMetricsService;

    @MockBean
    private ChatbotLogRepository chatbotLogRepository;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private CustomAuthenticationSuccessHandler successHandler;

    @Test
    @WithMockUser
    void obtenerTrending_RetornaLeaderboardDeRedis() throws Exception {
        List<Map<String, Object>> trending = List.of(
                Map.of("lugarId", 1L, "puntosTrending", 124.0),
                Map.of("lugarId", 2L, "puntosTrending", 98.0)
        );
        when(redisMetricsService.obtenerTopTrending(5)).thenReturn(trending);

        mockMvc.perform(get("/api/v1/metricas/trending?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].lugarId").value(1))
                .andExpect(jsonPath("$.data[0].puntosTrending").value(124.0));
    }

    @Test
    @WithMockUser
    void registrarVisita_IncrementaEnRedis() throws Exception {
        when(redisMetricsService.obtenerVisitas(5L)).thenReturn(42L);

        mockMvc.perform(post("/api/v1/metricas/visita/5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lugarId").value(5))
                .andExpect(jsonPath("$.data.totalVisitas").value(42));

        verify(redisMetricsService).registrarVisitaLugar(5L);
    }
}
