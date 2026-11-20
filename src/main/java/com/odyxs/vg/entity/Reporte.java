package com.odyxs.vg.entity;

import com.odyxs.vg.common.JsonMapConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "reportes", indexes = {
    @Index(name = "idx_rep_tipo_fecha", columnList = "tipo, fechaCreacion"),
    @Index(name = "idx_rep_estado_prio", columnList = "estado, prioridad"),
    @Index(name = "idx_rep_entidad", columnList = "entidadId, fechaCreacion")
})
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(unique = true, nullable = false, length = 64)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private TipoReporte tipo;

    @Column(length = 255)
    private String titulo;

    @Column(length = 1000)
    private String descripcion;

    private Long entidadId;

    @Column(length = 255)
    private String nombreEntidad;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Prioridad prioridad = Prioridad.MEDIA;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoReporte estado = EstadoReporte.NUEVO;

    private Long usuarioId;

    @Column(length = 150)
    private String usuarioEmail;

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> metadata = new HashMap<>();

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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public TipoReporte getTipo() { return tipo; }
    public void setTipo(TipoReporte tipo) { this.tipo = tipo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Long getEntidadId() { return entidadId; }
    public void setEntidadId(Long entidadId) { this.entidadId = entidadId; }

    public String getNombreEntidad() { return nombreEntidad; }
    public void setNombreEntidad(String nombreEntidad) { this.nombreEntidad = nombreEntidad; }

    public Prioridad getPrioridad() { return prioridad; }
    public void setPrioridad(Prioridad prioridad) { this.prioridad = prioridad; }

    public EstadoReporte getEstado() { return estado; }
    public void setEstado(EstadoReporte estado) { this.estado = estado; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Instant getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(Instant fechaResolucion) { this.fechaResolucion = fechaResolucion; }
}
