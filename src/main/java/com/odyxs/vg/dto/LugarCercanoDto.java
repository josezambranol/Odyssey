package com.odyxs.vg.dto;

public class LugarCercanoDto {

    private String id;
    private Long lugarId;
    private String nombre;
    private String descripcion;
    private String categoria;
    private String imagenUrl;
    private double latitud;
    private double longitud;
    private double distanciaKm;
    private String distanciaTexto;

    public LugarCercanoDto() {}

    public LugarCercanoDto(String id, Long lugarId, String nombre, String descripcion,
                           String categoria, String imagenUrl, double latitud, double longitud,
                           double distanciaKm) {
        this.id = id;
        this.lugarId = lugarId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.imagenUrl = imagenUrl;
        this.latitud = latitud;
        this.longitud = longitud;
        this.distanciaKm = Math.round(distanciaKm * 100.0) / 100.0;
        if (this.distanciaKm < 1.0) {
            this.distanciaTexto = (int) (this.distanciaKm * 1000) + " metros";
        } else {
            this.distanciaTexto = this.distanciaKm + " km";
        }
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

    public double getLatitud() { return latitud; }
    public void setLatitud(double latitud) { this.latitud = latitud; }

    public double getLongitud() { return longitud; }
    public void setLongitud(double longitud) { this.longitud = longitud; }

    public double getDistanciaKm() { return distanciaKm; }
    public void setDistanciaKm(double distanciaKm) { this.distanciaKm = distanciaKm; }

    public String getDistanciaTexto() { return distanciaTexto; }
    public void setDistanciaTexto(String distanciaTexto) { this.distanciaTexto = distanciaTexto; }
}
