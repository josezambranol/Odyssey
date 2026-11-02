package com.odyxs.vg.domain.mongo;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Document(collection = "reportes")
@CompoundIndexes({
    @CompoundIndex(name = "idx_tipo_fecha", def = "{'tipo': 1, 'fechaCreacion': -1}"),
    @CompoundIndex(name = "idx_estado_prioridad", def = "{'estado': 1, 'prioridad': 1}"),
    @CompoundIndex(name = "idx_entidad", def = "{'entidadId': 1, 'fechaCreacion': -1}")
})
public class Reporte {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    @Indexed
    private TipoReporte tipo;

    @TextIndexed
    private String titulo;

    @TextIndexed
    private String descripcion;

    @Indexed
    private Long entidadId; // ID referencial del Lugar/Actividad en MySQL

    private String nombreEntidad;

    @Indexed
    private Prioridad prioridad = Prioridad.MEDIA;

    @Indexed
    private EstadoReporte estado = EstadoReporte.NUEVO;

    private Long usuarioId; // ID del usuario que generó el reporte (opcional)

    private String usuarioEmail;

    private Map<String, Object> metadata = new HashMap<>();

    @CreatedDate
    @Indexed
    private Instant fechaCreacion;

    private Instant fechaResolucion;

    public enum TipoReporte {
        VISITA_TURISTICA,
        CONSULTA_IA,
        MODERACION_LUGAR,
        INCIDENCIA_TRANSPORTE,
        PROPUESTA_USUARIO,
        METRICA_SISTEMA
    }

    public enum Prioridad {
        BAJA, MEDIA, ALTA, CRITICA
    }

    public enum EstadoReporte {
        NUEVO, EN_PROCESO, RESUELTO, ARCHIVADO
    }

    public Reporte() {
        this.fechaCreacion = Instant.now();
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

    public TipoReporte getTipo() {
        return tipo;
    }

    public void setTipo(TipoReporte tipo) {
        this.tipo = tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public void setEntidadId(Long entidadId) {
        this.entidadId = entidadId;
    }

    public String getNombreEntidad() {
        return nombreEntidad;
    }

    public void setNombreEntidad(String nombreEntidad) {
        this.nombreEntidad = nombreEntidad;
    }

    public Prioridad getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(Prioridad prioridad) {
        this.prioridad = prioridad;
    }

    public EstadoReporte getEstado() {
        return estado;
    }

    public void setEstado(EstadoReporte estado) {
        this.estado = estado;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public void setUsuarioEmail(String usuarioEmail) {
        this.usuarioEmail = usuarioEmail;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Instant getFechaResolucion() {
        return fechaResolucion;
    }

    public void setFechaResolucion(Instant fechaResolucion) {
        this.fechaResolucion = fechaResolucion;
    }
}
