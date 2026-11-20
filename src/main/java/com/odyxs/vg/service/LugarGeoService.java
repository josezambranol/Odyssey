package com.odyxs.vg.service;

import com.odyxs.vg.dto.LugarCercanoDto;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.LugarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class LugarGeoService {

    private static final Logger log = LoggerFactory.getLogger(LugarGeoService.class);
    private static final double RADIO_TIERRA_KM = 6371.0;

    @Autowired(required = false)
    private LugarRepository lugarRepository;

    /**
     * Búsqueda geoespacial sobre el catálogo unificado en MySQL usando la fórmula Haversine.
     * Retorna lugares aprobados con coordenadas dentro del radio especificado, ordenados por proximidad.
     */
    public List<LugarCercanoDto> buscarLugaresCercanos(double longitud, double latitud, double radioKm) {
        if (lugarRepository == null) return List.of();

        List<Lugar> aprobados = lugarRepository.findByEstado(Lugar.Estado.APROBADO);

        List<LugarCercanoDto> cercanos = aprobados.stream()
                .filter(l -> l.getLatitud() != null && l.getLongitud() != null)
                .map(l -> {
                    double distKm = calcularDistanciaHaversineKm(latitud, longitud, l.getLatitud(), l.getLongitud());
                    return new AbstractMapEntry(l, distKm);
                })
                .filter(entry -> entry.distKm <= radioKm)
                .sorted(Comparator.comparingDouble(entry -> entry.distKm))
                .map(entry -> toDto(entry.lugar, entry.distKm))
                .toList();

        log.info("[GEO-SQL] Búsqueda cerca de [{}, {}] en radio de {} km. Resultados encontrados: {}",
                longitud, latitud, radioKm, cercanos.size());
        return cercanos;
    }

    public static double calcularDistanciaHaversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RADIO_TIERRA_KM * c;
    }

    private LugarCercanoDto toDto(Lugar l, double distKm) {
        String catNombre = (l.getCategoria() != null && l.getCategoria().getNombre() != null)
                ? formatearCategoriaMapa(l.getCategoria().getNombre())
                : "Sitios Históricos";
        return new LugarCercanoDto(
                String.valueOf(l.getId()),
                l.getId(),
                l.getNombre(),
                l.getDescripcion() != null ? l.getDescripcion() : l.getUbicacion(),
                catNombre,
                l.getImagenEfectiva(),
                l.getLatitud(),
                l.getLongitud(),
                distKm
        );
    }

    private String formatearCategoriaMapa(String raw) {
        return switch (raw.toUpperCase()) {
            case "PLAYAS" -> "Playas";
            case "MUSEOS" -> "Museos";
            case "HOTELES" -> "Hoteles";
            case "GASTRONOMÍA", "GASTRONOMIA" -> "Gastronomía";
            case "CINES", "FESTIVALES" -> "Cultura y Arte";
            default -> "Sitios Históricos";
        };
    }

    private record AbstractMapEntry(Lugar lugar, double distKm) {}
}
