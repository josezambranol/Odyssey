package com.odyxs.vg.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "actividades")
public class Actividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    private String duracion;

    @Column(name = "precio_aprox")
    private String precioAprox;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaActividad categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PENDIENTE;

    @Column(nullable = false)
    private boolean esOficial = false;

    public enum CategoriaActividad { TOUR, DEPORTE, CULTURA, GASTRONOMIA, NATURALEZA }

    public enum Estado { PENDIENTE, APROBADO, RECHAZADO }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getDuracion() { return duracion; }
    public void setDuracion(String duracion) { this.duracion = duracion; }
    public String getPrecioAprox() { return precioAprox; }
    public void setPrecioAprox(String precioAprox) { this.precioAprox = precioAprox; }
    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }
    public CategoriaActividad getCategoria() { return categoria; }
    public void setCategoria(CategoriaActividad categoria) { this.categoria = categoria; }
    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }
    public boolean isEsOficial() { return esOficial; }
    public void setEsOficial(boolean esOficial) { this.esOficial = esOficial; }

    public String getImagenEfectiva() {
        if (imagenUrl != null && !imagenUrl.isBlank()) return imagenUrl;
        return "/img/act-gastronomia.jpg";
    }
}
