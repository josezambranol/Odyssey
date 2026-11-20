package com.odyxs.vg.service;

import com.odyxs.vg.dto.LugarCercanoDto;
import com.odyxs.vg.entity.Categoria;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.LugarRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LugarGeoServiceTest {

    @Mock
    private LugarRepository lugarRepository;

    @InjectMocks
    private LugarGeoService lugarGeoService;

    @Test
    void buscarLugaresCercanos_CalculaHaversineSobreMySQL() {
        Categoria cat = new Categoria();
        cat.setNombre("SITIOS HISTÓRICOS");

        Lugar castillo = new Lugar();
        castillo.setId(10L);
        castillo.setNombre("Castillo de San Felipe");
        castillo.setDescripcion("Fortaleza militar");
        castillo.setCategoria(cat);
        castillo.setImagenUrl("/img/hero-castillo.jpg");
        castillo.setLatitud(10.4225);
        castillo.setLongitud(-75.5398);
        castillo.setEstado(Lugar.Estado.APROBADO);

        Lugar baru = new Lugar();
        baru.setId(1L);
        baru.setNombre("Playa Blanca");
        baru.setCategoria(cat);
        baru.setLatitud(10.2281);
        baru.setLongitud(-75.6148);
        baru.setEstado(Lugar.Estado.APROBADO);

        when(lugarRepository.findByEstado(Lugar.Estado.APROBADO)).thenReturn(List.of(castillo, baru));

        // Búsqueda desde Torre del Reloj con radio 2 km (solo debe incluir Castillo, no Barú a ~22 km)
        List<LugarCercanoDto> cercanos = lugarGeoService.buscarLugaresCercanos(-75.5489, 10.4233, 2.0);

        assertNotNull(cercanos);
        assertEquals(1, cercanos.size());
        assertEquals("Castillo de San Felipe", cercanos.get(0).getNombre());
        assertTrue(cercanos.get(0).getDistanciaKm() > 0.5 && cercanos.get(0).getDistanciaKm() < 1.5);
    }
}
