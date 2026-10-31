package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.Itinerario;
import com.odyxs.vg.dto.GenerarItinerarioRequest;
import com.odyxs.vg.repository.mongo.ItinerarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItinerarioServiceTest {

    @Mock
    private ItinerarioRepository itinerarioRepository;

    @Mock
    private GeminiAiService geminiAiService;

    @InjectMocks
    private ItinerarioService itinerarioService;

    @Test
    void generarItinerario_3DiasCultural_GeneraPlanCompleto() {
        GenerarItinerarioRequest req = new GenerarItinerarioRequest();
        req.setDias(3);
        req.setEstiloViaje("CULTURAL");
        req.setPresupuesto("MEDIO");
        req.setUsuarioId(1L);

        when(itinerarioRepository.save(any(Itinerario.class))).thenAnswer(i -> i.getArgument(0));

        Itinerario res = itinerarioService.generarItinerario(req);

        assertNotNull(res);
        assertNotNull(res.getCodigo());
        assertTrue(res.getCodigo().startsWith("ITIN-"));
        assertEquals(3, res.getPlanDias().size());
        assertEquals("Centro Histórico Colonial y Puesta de Sol en las Murallas", res.getPlanDias().get(0).getTituloDia());
        assertEquals("Fortaleza Imperial y el Alma Bohemia de Getsemaní", res.getPlanDias().get(1).getTituloDia());
        assertEquals("Paraíso Insular: Islas del Rosario y Mar Turquesa", res.getPlanDias().get(2).getTituloDia());
        verify(itinerarioRepository, times(1)).save(any(Itinerario.class));
    }

    @Test
    void buscarPorCodigo_EncuentraItinerario() {
        Itinerario it = new Itinerario();
        it.setCodigo("ITIN-12345");
        when(itinerarioRepository.findByCodigo("ITIN-12345")).thenReturn(Optional.of(it));

        Optional<Itinerario> res = itinerarioService.buscarPorCodigo("ITIN-12345");

        assertTrue(res.isPresent());
        assertEquals("ITIN-12345", res.get().getCodigo());
    }
}
