package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.common.ApiResponse;
import com.odyxs.vg.domain.mongo.Itinerario;
import com.odyxs.vg.dto.GenerarItinerarioRequest;
import com.odyxs.vg.service.ItinerarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/itinerarios")
@Tag(name = "Itinerarios con IA (Gemini & NoSQL)", description = "Generación inteligente de planes turísticos personalizados en Cartagena")
public class ItinerarioRestController {

    @Autowired
    private ItinerarioService itinerarioService;

    @Operation(summary = "✨ Generar plan de viaje inteligente con IA (Gemini)",
               description = "Construye un itinerario detallado día por día (Mañana, Tarde, Noche) según días, presupuesto y estilo de viaje, almacenándolo en MongoDB.")
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

    @Operation(summary = "Obtener itinerarios guardados de un usuario")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ApiResponse<List<Itinerario>>> obtenerPorUsuario(@PathVariable Long usuarioId) {
        List<Itinerario> lista = itinerarioService.buscarPorUsuario(usuarioId);
        return ResponseEntity.ok(ApiResponse.ok("Itinerarios del usuario", lista));
    }
}
