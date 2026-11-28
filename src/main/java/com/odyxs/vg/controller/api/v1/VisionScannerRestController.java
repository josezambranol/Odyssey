package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.service.GeminiVisionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vision")
@Tag(name = "Visión Multimodal & Escudo Anti-Sobrecobros v5.0", description = "Reconocimiento fotográfico patrimonial y auditoría de precios turísticos conforme a la Ley 1480 de 2011")
public class VisionScannerRestController {

    private final GeminiVisionService visionService;

    public VisionScannerRestController(GeminiVisionService visionService) {
        this.visionService = visionService;
    }

    @GetMapping("/tarifas-referencia")
    @Operation(summary = "Listar precios oficiales y rangos justos de referencia en Cartagena (COP)")
    public ResponseEntity<ApiResponse<List<GeminiVisionService.TarifaReferenciaDto>>> listarTarifasReferencia() {
        return ResponseEntity.ok(ApiResponse.ok(visionService.obtenerTarifasReferencia()));
    }

    @PostMapping(value = "/analizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analizar fotografía de monumento o carta/cuenta con IA Multimodal y auditor local")
    public ResponseEntity<ApiResponse<GeminiVisionService.ResultadoVisionDto>> analizarImagenOTexto(
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            @RequestParam(value = "modo", defaultValue = "PRECIOS") String modo,
            @RequestParam(value = "descripcion", defaultValue = "") String descripcion) {
        try {
            GeminiVisionService.ResultadoVisionDto resultado =
                    visionService.analizarEntradaMultimodal(imagen, modo, descripcion);
            return ResponseEntity.ok(ApiResponse.ok(resultado));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }
}
