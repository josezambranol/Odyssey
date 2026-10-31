package com.odyxs.vg.service;

import com.odyxs.vg.domain.mongo.LugarGeoDocument;
import com.odyxs.vg.dto.LugarCercanoDto;
import com.odyxs.vg.repository.mongo.LugarGeoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LugarGeoService {

    private static final Logger log = LoggerFactory.getLogger(LugarGeoService.class);

    @Autowired(required = false)
    private LugarGeoRepository lugarGeoRepository;

    /**
     * Búsqueda geoespacial nativa en MongoDB usando índices 2dsphere.
     * Retorna lugares dentro del radio especificado, ordenados por proximidad exacta.
     */
    public List<LugarCercanoDto> buscarLugaresCercanos(double longitud, double latitud, double radioKm) {
        if (lugarGeoRepository == null) return List.of();

        // Inicializar datos geoespaciales si la colección está vacía
        asegurarCatalogoGeoespacial();

        Point puntoUsuario = new Point(longitud, latitud);
        Distance radio = new Distance(radioKm, Metrics.KILOMETERS);

        GeoResults<LugarGeoDocument> results = lugarGeoRepository.findByUbicacionNear(puntoUsuario, radio);
        List<LugarCercanoDto> dtos = new ArrayList<>();

        if (results != null) {
            for (GeoResult<LugarGeoDocument> res : results) {
                LugarGeoDocument doc = res.getContent();
                double distanciaKm = res.getDistance().getValue();
                dtos.add(new LugarCercanoDto(
                        doc.getId(),
                        doc.getLugarId(),
                        doc.getNombre(),
                        doc.getDescripcion(),
                        doc.getCategoria(),
                        doc.getImagenUrl(),
                        doc.getLatitud(),
                        doc.getLongitud(),
                        distanciaKm
                ));
            }
        }

        log.info("[GEO] Búsqueda cerca de [{}, {}] en radio de {} km. Resultados encontrados: {}",
                longitud, latitud, radioKm, dtos.size());
        return dtos;
    }

    public synchronized void asegurarCatalogoGeoespacial() {
        if (lugarGeoRepository == null) return;
        try {
            if (lugarGeoRepository.count() == 0) {
                log.info("[GEO] Inicializando catálogo con coordenadas geográficas de Cartagena...");
                List<LugarGeoDocument> sitios = List.of(
                        new LugarGeoDocument(1L, "Castillo de San Felipe de Barajas",
                                "La mayor fortaleza colonial de América española.", "Historia",
                                "/uploads/lugares/castillo.jpg", -75.5398, 10.4225),

                        new LugarGeoDocument(2L, "Torre del Reloj y Plaza de la Paz",
                                "Puerta de entrada principal a la Ciudad Amurallada.", "Cultura",
                                "/img/about-torre-reloj.jpg", -75.5489, 10.4233),

                        new LugarGeoDocument(3L, "Plaza de la Trinidad y Getsemaní",
                                "El epicentro cultural bohemio y gastronómico más vibrante de Cartagena.", "Bohemio",
                                "/img/act-getsemani.jpg", -75.5458, 10.4201),

                        new LugarGeoDocument(4L, "Baluarte de Santo Domingo",
                                "Muralla histórica con la mejor vista de la puesta de sol sobre el Caribe.", "Historia",
                                "/img/hero-murallas.jpg", -75.5518, 10.4241),

                        new LugarGeoDocument(5L, "Playa de Bocagrande",
                                "Playas urbanas frente a modernos rascacielos y restaurantes de mariscos.", "Playa",
                                "/img/act-playa-bocagrande.jpg", -75.5567, 10.4022),

                        new LugarGeoDocument(6L, "Convento de la Popa",
                                "El punto más alto de Cartagena con mirador de 360 grados y capilla colonial.", "Mirador",
                                "/img/hero-getsemani.jpg", -75.5244, 10.4192),

                        new LugarGeoDocument(7L, "Playa Blanca (Isla de Barú)",
                                "Aguas cristalinas y arena blanca en la península de Barú.", "Playa",
                                "/img/act-islas-rosario.jpg", -75.6148, 10.2281)
                );
                lugarGeoRepository.saveAll(sitios);
                log.info("[GEO] 7 puntos emblemáticos georreferenciados en MongoDB.");
            }
        } catch (Exception ex) {
            log.warn("[GEO] No se pudo inicializar catálogo geoespacial: {}", ex.getMessage());
        }
    }
}
