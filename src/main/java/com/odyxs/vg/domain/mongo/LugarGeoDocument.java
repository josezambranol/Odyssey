package com.odyxs.vg.domain.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexType;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lugares_geo")
public class LugarGeoDocument {

    @Id
    private String id;

    @Indexed
    private Long lugarId; // ID referencial en MySQL

    private String nombre;

    private String descripcion;

    private String categoria;

    private String imagenUrl;

    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint ubicacion; // [longitud, latitud]

    private double latitud;
    private double longitud;

    public LugarGeoDocument() {}

    public LugarGeoDocument(Long lugarId, String nombre, String descripcion, String categoria,
                            String imagenUrl, double longitud, double latitud) {
        this.lugarId = lugarId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
        this.longitud = longitud;
        this.latitud = latitud;
        this.ubicacion = new GeoJsonPoint(longitud, latitud);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getLugarId() { return lugarId; }
    public void setLugarId(Long lugarId) { this.lugarId = lugarId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public GeoJsonPoint getUbicacion() { return ubicacion; }
    public void setUbicacion(GeoJsonPoint ubicacion) { this.ubicacion = ubicacion; }

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }
}
