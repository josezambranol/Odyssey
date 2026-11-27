package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.entity.SuscripcionPush;
import com.odyxs.vg.service.WebPushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/push")
@Tag(name = "Notificaciones Web Push PWA v4.0", description = "Suscripción VAPID P-256 y alertas nativas de eventos culturales en Cartagena")
public class PushNotificationRestController {

    private final WebPushNotificationService pushService;

    public PushNotificationRestController(WebPushNotificationService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/vapid-public-key")
    @Operation(summary = "Obtener clave pública VAPID P-256 (Base64URL de 65 bytes)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> obtenerClaveVapid(
            @RequestParam(required = false) Long lugarId) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "publicKey", pushService.obtenerClavePublicaVapid(),
                "subject", pushService.obtenerSubjectVapid(),
                "dispositivosActivos", pushService.contarDispositivosActivos(lugarId)
        )));
    }

    @PostMapping("/subscribe")
    @Operation(summary = "Registrar suscripción Push del navegador/PWA")
    public ResponseEntity<ApiResponse<Map<String, Object>>> suscribirDispositivo(
            @RequestBody WebPushNotificationService.SuscripcionPushRequest request,
            HttpSession session) {
        if (request == null || request.endpoint() == null || request.endpoint().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Endpoint de suscripción requerido."));
        }
        Long usuarioId = null;
        if (session != null && session.getAttribute("usuarioId") != null) {
            usuarioId = Long.valueOf(session.getAttribute("usuarioId").toString());
        }
        SuscripcionPush guardada = pushService.registrarSuscripcion(request, usuarioId);
        long total = pushService.contarDispositivosActivos(guardada.getLugarId());

        return ResponseEntity.ok(ApiResponse.ok("Notificaciones Web Push activadas en este dispositivo.", Map.of(
                "suscripcionId", guardada.getId(),
                "lugarId", guardada.getLugarId() != null ? guardada.getLugarId() : 0L,
                "dispositivosActivos", total
        )));
    }

    @GetMapping("/alertas-recientes")
    @Operation(summary = "Consultar últimas alertas culturales emitidas para la PWA")
    public ResponseEntity<ApiResponse<List<WebPushNotificationService.AlertaPushDto>>> obtenerAlertasRecientes(
            @RequestParam(required = false) Long lugarId) {
        return ResponseEntity.ok(ApiResponse.ok(pushService.obtenerAlertasRecientes(lugarId)));
    }
}
