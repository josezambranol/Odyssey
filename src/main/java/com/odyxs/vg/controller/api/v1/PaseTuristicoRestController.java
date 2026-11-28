package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.service.QrPassCryptoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pases")
@Tag(name = "Pases Turísticos QR Firmados & RNT v5.0", description = "Emisión y verificación criptográfica sin estado con HMAC-SHA256 y control de RNT (Ley 2068 de 2020)")
public class PaseTuristicoRestController {

    private final QrPassCryptoService qrPassCryptoService;

    public PaseTuristicoRestController(QrPassCryptoService qrPassCryptoService) {
        this.qrPassCryptoService = qrPassCryptoService;
    }

    @PostMapping("/emitir")
    @Operation(summary = "Emitir un Pase Turístico digital firmado con HMAC-SHA256 y sello QR vectorial")
    public ResponseEntity<ApiResponse<QrPassCryptoService.PaseEmitidoDto>> emitirPase(
            @RequestBody QrPassCryptoService.SolicitudPaseRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(qrPassCryptoService.emitirPaseFirmado(request)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
        }
    }

    @GetMapping("/verificar")
    @Operation(summary = "Verificar en tiempo constante la firma criptográfica HMAC-SHA256 y vigencia de un Pase QR")
    public ResponseEntity<ApiResponse<QrPassCryptoService.ResultadoVerificacionDto>> verificarPase(
            @RequestParam("token") String token) {
        return ResponseEntity.ok(ApiResponse.ok(qrPassCryptoService.verificarPaseFirmado(token)));
    }
}
