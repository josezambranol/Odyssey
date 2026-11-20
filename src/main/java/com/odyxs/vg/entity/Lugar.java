package com.odyxs.vg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "lugares")
public class Lugar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(length = 1000)
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false)
    private String ubicacion;

    @Column(length = 1000)
    private String urlMapa;

    @Column(length = 500)
    private String imagenUrl;

    private Double latitud;

    private Double longitud;

    private Long visitas = 0L;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Estado estado = Estado.PENDIENTE;

    @Column(nullable = false)
    private boolean esOficial = false;

    public enum Estado {
        PENDIENTE, APROBADO, RECHAZADO
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Categoria getCategoria() { return categoria; }
    public void setCategoria(Categoria categoria) { this.categoria = categoria; }

    public String getUbicacion() { return ubicacion; }
    public void setUbicacion(String ubicacion) { this.ubicacion = ubicacion; }

    public String getUrlMapa() { return urlMapa; }
    public void setUrlMapa(String urlMapa) { this.urlMapa = urlMapa; }

    public String getImagenUrl() { return imagenUrl; }
    public void setImagenUrl(String imagenUrl) { this.imagenUrl = imagenUrl; }

    public Estado getEstado() { return estado; }
    public void setEstado(Estado estado) { this.estado = estado; }

    public boolean isEsOficial() { return esOficial; }
    public void setEsOficial(boolean esOficial) { this.esOficial = esOficial; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Long getVisitas() { return visitas; }
    public void setVisitas(Long visitas) { this.visitas = visitas; }

    public String getImagenEfectiva() {
        if (imagenUrl != null && !imagenUrl.isBlank()) return imagenUrl;
        return "/img/hero-murallas.jpg";
    }
}