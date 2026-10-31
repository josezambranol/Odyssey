package com.odyxs.vg.dto;

import com.odyxs.vg.domain.mongo.Reporte;

import java.time.Instant;

public class ReporteResumenDto {

    private String id;
    private String codigo;
    private Reporte.TipoReporte tipo;
    private String titulo;
    private String nombreEntidad;
    private Reporte.Prioridad prioridad;
    private Reporte.EstadoReporte estado;
    private Instant fechaCreacion;

    public ReporteResumenDto() {}

    public ReporteResumenDto(Reporte r) {
        this.id = r.getId();
        this.codigo = r.getCodigo();
        this.tipo = r.getTipo();
        this.titulo = r.getTitulo();
        this.nombreEntidad = r.getNombreEntidad();
        this.prioridad = r.getPrioridad();
        this.estado = r.getEstado();
        this.fechaCreacion = r.getFechaCreacion();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public Reporte.TipoReporte getTipo() {
        return tipo;
    }

    public void setTipo(Reporte.TipoReporte tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getNombreEntidad() {
        return nombreEntidad;
    }

    public void setNombreEntidad(String nombreEntidad) {
        this.nombreEntidad = nombreEntidad;
    }

    public Reporte.Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Reporte.Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public Reporte.EstadoReporte getEstado() {
        return estado;
    }

    public void setEstado(Reporte.EstadoReporte estado) {
        this.estado = estado;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
