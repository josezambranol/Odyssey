package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.LugarGeoDocument;
import com.odyxs.vg.dto.LugarCercanoDto;
import com.odyxs.vg.repository.mongo.LugarGeoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LugarGeoServiceTest {

    @Mock
    private LugarGeoRepository lugarGeoRepository;

    @InjectMocks
    private LugarGeoService lugarGeoService;

    @Test
    void buscarLugaresCercanos_RetornaLugaresConDistancia() {
        LugarGeoDocument doc = new LugarGeoDocument(
                1L, "Castillo de San Felipe", "Fortaleza militar", "Historia", "/img/castillo.jpg", -75.5398, 10.4225
        );
        GeoResult<LugarGeoDocument> geoResult = new GeoResult<>(doc, new Distance(0.85, org.springframework.data.geo.Metrics.KILOMETERS));
        GeoResults<LugarGeoDocument> geoResults = new GeoResults<>(List.of(geoResult));

        when(lugarGeoRepository.count()).thenReturn(7L);
        when(lugarGeoRepository.findByUbicacionNear(any(Point.class), any(Distance.class))).thenReturn(geoResults);

        List<LugarCercanoDto> cercanos = lugarGeoService.buscarLugaresCercanos(-75.5489, 10.4233, 2.0);

        assertNotNull(cercanos);
        assertEquals(1, cercanos.size());
        assertEquals("Castillo de San Felipe", cercanos.get(0).getNombre());
        assertEquals(0.85, cercanos.get(0).getDistanciaKm());
        assertEquals("850 metros", cercanos.get(0).getDistanciaTexto());
    }
}
