package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.service.MarineWeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clima")
@Tag(name = "Clima Marino & UV v4.0", description = "Pronóstico costero, índice UV y semáforo náutico para Islas del Rosario y Cartagena")
public class ClimaMarinoRestController {

    private final MarineWeatherService marineWeatherService;

    public ClimaMarinoRestController(MarineWeatherService marineWeatherService) {
        this.marineWeatherService = marineWeatherService;
    }

    @GetMapping("/marino")
    @Operation(
            summary = "Obtener estado climático marino, UV y semáforo de zarpe",
            description = "Retorna temperatura, sensación térmica, viento en km/h y nudos, radiación UV y semáforo náutico (VERDE/AMARILLO/ROJO) con caché TTL de 15 minutos."
    )
    public ResponseEntity<ApiResponse<MarineWeatherService.ReporteClimaMarinoDto>> obtenerClimaMarino() {
        return ResponseEntity.ok(ApiResponse.ok(marineWeatherService.obtenerPronosticoActual()));
    }
}
