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

        // Inicializar datos geoespaciales si la colección está incompleta
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
            if (lugarGeoRepository.count() < 20) {
                log.info("[GEO] Inicializando catálogo maestro de 26 puntos georreferenciados de Cartagena...");
                lugarGeoRepository.deleteAll();

                List<LugarGeoDocument> sitios = List.of(
                        new LugarGeoDocument(1L, "Castillo de San Felipe de Barajas",
                                "La mayor fortaleza colonial de América española.", "Sitios Históricos",
                                "/uploads/lugares/castillo.jpg", -75.5398, 10.4225),

                        new LugarGeoDocument(2L, "Torre del Reloj y Plaza de los Coches",
                                "Puerta de entrada principal a la Ciudad Amurallada.", "Sitios Históricos",
                                "/img/about-torre-reloj.jpg", -75.5489, 10.4233),

                        new LugarGeoDocument(3L, "Baluarte de Santo Domingo y Murallas",
                                "Tramo defensivo sobre el mar Caribe para ver el atardecer.", "Sitios Históricos",
                                "/img/hero-murallas.jpg", -75.5518, 10.4241),

                        new LugarGeoDocument(4L, "Las Bóvedas de Santa Clara",
                                "23 bóvedas y 47 arcos coloniales, hoy mercado de artesanías.", "Sitios Históricos",
                                "/img/hero-murallas.jpg", -75.5448, 10.4285),

                        new LugarGeoDocument(5L, "Palacio de la Inquisición (MUHCA)",
                                "Mansión colonial sede del Tribunal del Santo Oficio y museo histórico.", "Museos",
                                "/img/about-torre-reloj.jpg", -75.5502, 10.4237),

                        new LugarGeoDocument(6L, "Convento y Mirador de la Popa",
                                "Punto más alto de Cartagena a 150m con vista panorámica de 360°.", "Sitios Históricos",
                                "/img/hero-getsemani.jpg", -75.5244, 10.4192),

                        new LugarGeoDocument(7L, "Museo del Oro Zenú",
                                "Colección precolombina de orfebrería y tejidos hidráulicos Zenú.", "Museos",
                                "/img/about-torre-reloj.jpg", -75.5508, 10.4235),

                        new LugarGeoDocument(8L, "Museo Naval del Caribe",
                                "Historia de las batallas marítimas del Caribe y Armada Nacional.", "Museos",
                                "/img/hero-murallas.jpg", -75.5515, 10.4228),

                        new LugarGeoDocument(9L, "Santuario y Claustro de San Pedro Claver",
                                "Templo del siglo XVII donde descansan los restos del apóstol de los esclavos.", "Sitios Históricos",
                                "/img/about-torre-reloj.jpg", -75.5504, 10.4222),

                        new LugarGeoDocument(10L, "Teatro Adolfo Mejía (Teatro Heredia)",
                                "Joyel arquitectónico de 1911 con influencias de la ópera italiana.", "Cultura y Arte",
                                "/img/about-torre-reloj.jpg", -75.5492, 10.4265),

                        new LugarGeoDocument(11L, "Casa Museo Rafael Núñez",
                                "Casona campestre de madera donde vivió el autor del Himno Nacional.", "Museos",
                                "/img/about-torre-reloj.jpg", -75.5422, 10.4320),

                        new LugarGeoDocument(12L, "Plaza de la Trinidad y Getsemaní",
                                "Epicentro bohemio, arte urbano, gastronomía callejera y champeta.", "Cultura y Arte",
                                "/img/act-getsemani.jpg", -75.5458, 10.4201),

                        new LugarGeoDocument(13L, "Playa Blanca (Península de Barú)",
                                "Aguas turquesas y arena coralina en la península de Barú.", "Playas",
                                "/img/hero-playa-blanca.jpg", -75.6148, 10.2281),

                        new LugarGeoDocument(14L, "Parque Nacional Natural Islas del Rosario",
                                "Archipiélago de 28 islas de aguas cristalinas y arrecifes coralinos.", "Naturaleza y Aventura",
                                "/img/act-islas-rosario.jpg", -75.7485, 10.1782),

                        new LugarGeoDocument(15L, "Playas de Bocagrande y Castillogrande",
                                "Playas urbanas frente a modernos rascacielos con paseo marítimo.", "Playas",
                                "/img/act-playa-bocagrande.jpg", -75.5567, 10.4022),

                        new LugarGeoDocument(16L, "Playas de La Boquilla y Manglares",
                                "Pueblo de pescadores afrodescendientes, kitesurf y ecoturismo en canoa.", "Naturaleza y Aventura",
                                "/img/act-kayak-manglares.jpg", -75.4950, 10.4855),

                        new LugarGeoDocument(17L, "Isla de Cholón (Barú)",
                                "Ensenada náutica con ambiente festivo y quioscos sobre el agua.", "Playas",
                                "/img/act-islas-rosario.jpg", -75.6833, 10.1833),

                        new LugarGeoDocument(18L, "Fuerte de San Fernando (Bocachica)",
                                "Fortaleza semicircular de 1753 en la entrada marítima a la bahía.", "Sitios Históricos",
                                "/img/hero-murallas.jpg", -75.5847, 10.3208),

                        new LugarGeoDocument(19L, "Mercado de Bazurto",
                                "Mercado popular sensorial con gastronomía criolla y champeta.", "Gastronomía",
                                "/img/act-gastronomia.jpg", -75.5327, 10.4097),

                        new LugarGeoDocument(20L, "Portal de los Dulces",
                                "Pasaje colonial con dulceras tradicionales y manjares costeños.", "Gastronomía",
                                "/img/gastro-arepa.jpg", -75.5484, 10.4236),

                        new LugarGeoDocument(21L, "La Cevichería",
                                "Célebre restaurante de mariscos en Getsemaní visitado por Anthony Bourdain.", "Gastronomía",
                                "/img/gastro-ceviche.jpg", -75.5458, 10.4262),

                        new LugarGeoDocument(22L, "Restaurante Celele",
                                "Entre los 50 mejores restaurantes de América Latina; cocina del Caribe.", "Gastronomía",
                                "/img/gastro-arroz-mariscos.jpg", -75.5448, 10.4215),

                        new LugarGeoDocument(23L, "Café del Mar",
                                "Ubicado sobre el Baluarte de Santo Domingo para ver la puesta de sol.", "Gastronomía",
                                "/img/hero-murallas.jpg", -75.5521, 10.4243),

                        new LugarGeoDocument(24L, "Sofitel Legend Santa Clara",
                                "Antiguo convento clarisa de 1621 convertido en hotel de ultralujo.", "Hoteles",
                                "/img/hotel-sofitel.jpg", -75.5463, 10.4278),

                        new LugarGeoDocument(25L, "Hotel Charleston Santa Teresa",
                                "Convento carmelita del siglo XVII frente a las murallas coloniales.", "Hoteles",
                                "/img/hotel-charleston.jpg", -75.5516, 10.4229),

                        new LugarGeoDocument(26L, "Bastión Luxury Hotel",
                                "Mansión colonial virreinal en el corazón de la Ciudad Amurallada.", "Hoteles",
                                "/img/hotel-bastion.jpg", -75.5492, 10.4248)
                );
                lugarGeoRepository.saveAll(sitios);
                log.info("[GEO] 26 puntos emblemáticos de Cartagena georreferenciados con éxito en MongoDB.");
            }
        } catch (Exception ex) {
            log.warn("[GEO] No se pudo inicializar catálogo geoespacial: {}", ex.getMessage());
        }
    }
}
