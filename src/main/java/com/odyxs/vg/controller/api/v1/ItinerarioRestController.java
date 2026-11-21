package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.entity.Itinerario;
import com.odyxs.vg.dto.GenerarItinerarioRequest;
import com.odyxs.vg.entity.Usuario;
import com.odyxs.vg.security.CustomUserDetails;
import com.odyxs.vg.service.ItinerarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itinerarios")
@Tag(name = "Itinerarios con IA", description = "Generación inteligente de planes turísticos personalizados en Cartagena")
public class ItinerarioRestController {

    @Autowired
    private ItinerarioService itinerarioService;

    @Operation(summary = "Generar plan de viaje inteligente",
               description = "Construye un itinerario detallado día por día (Mañana, Tarde, Noche) según días, presupuesto y estilo de viaje, almacenándolo en MySQL.")
    @PostMapping("/generar")
    public ResponseEntity<ApiResponse<Itinerario>> generar(@Valid @RequestBody GenerarItinerarioRequest request) {
        Itinerario itinerario = itinerarioService.generarItinerario(request);
        return ResponseEntity.ok(ApiResponse.ok("Itinerario generado con éxito", itinerario));
    }

    @Operation(summary = "Obtener itinerario por ID o Código único")
    @GetMapping("/{idOCodigo}")
    public ResponseEntity<ApiResponse<Itinerario>> obtener(@PathVariable String idOCodigo) {
        return itinerarioService.buscarPorId(idOCodigo)
                .or(() -> itinerarioService.buscarPorCodigo(idOCodigo))
                .map(it -> ResponseEntity.ok(ApiResponse.ok(it)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Obtener itinerarios guardados de un usuario (requiere sesión del titular o ADMIN)")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<Itinerario>>> obtenerPorUsuario(
            @PathVariable Long usuarioId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Debes iniciar sesión para consultar tus itinerarios."));
        }
        boolean esAdmin = userDetails.getUsuario() != null && userDetails.getUsuario().getRol() == Usuario.Rol.ADMIN;
        if (!esAdmin && !usuarioId.equals(userDetails.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("No tienes permisos para consultar itinerarios de otro usuario."));
        }
        List<Itinerario> lista = itinerarioService.buscarPorUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok("Itinerarios del usuario", lista));
    }
}
