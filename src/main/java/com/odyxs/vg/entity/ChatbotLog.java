package com.odyxs.vg.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chatbot_logs", indexes = {
    @Index(name = "idx_chat_fecha_cat", columnList = "fechaCreacion, categoria")
})
public class ChatbotLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(length = 2000)
    private String mensajeUsuario;

    @Column(columnDefinition = "TEXT")
    private String respuestaIA;

    private long tiempoRespuestaMs;

    @Column(length = 100)
    private String modelo = "gemini-1.5-flash";

    @Column(length = 10)
    private String idioma = "es";

    @Column(length = 64)
    private String categoria;

    @Column(length = 64)
    private String ipRemota;

    private boolean exito = true;

    @Column(length = 500)
    private String errorDetalle;

    private Instant fechaCreacion;

    public ChatbotLog() {
        this.fechaCreacion = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMensajeUsuario() { return mensajeUsuario; }
    public void setMensajeUsuario(String mensajeUsuario) { this.mensajeUsuario = mensajeUsuario; }

    public String getRespuestaIA() { return respuestaIA; }
    public void setRespuestaIA(String respuestaIA) { this.respuestaIA = respuestaIA; }

    public long getTiempoRespuestaMs() { return tiempoRespuestaMs; }
    public void setTiempoRespuestaMs(long tiempoRespuestaMs) { this.tiempoRespuestaMs = tiempoRespuestaMs; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getIpRemota() { return ipRemota; }
    public void setIpRemota(String ipRemota) { this.ipRemota = ipRemota; }

    public boolean isExito() { return exito; }
    public void setExito(boolean exito) { this.exito = exito; }

    public String getErrorDetalle() { return errorDetalle; }
    public void setErrorDetalle(String errorDetalle) { this.errorDetalle = errorDetalle; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
