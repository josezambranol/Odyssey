package com.odyxs.vg.domain.mongo;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "chatbot_logs")
@CompoundIndexes({
    @CompoundIndex(name = "idx_chat_fecha_cat", def = "{'fechaCreacion': -1, 'categoria': 1}")
})
public class ChatbotLog {

    @Id
    private String id;

    private String mensajeUsuario;

    private String respuestaIA;

    private long tiempoRespuestaMs;

    private String modelo = "gemini-1.5-flash";

    private String idioma = "es";

    @Indexed
    private String categoria;

    private String ipRemota;

    private boolean exito = true;

    private String errorDetalle;

    @CreatedDate
    @Indexed
    private Instant fechaCreacion;

    public ChatbotLog() {
        this.fechaCreacion = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMensajeUsuario() {
        return mensajeUsuario;
    }

    public void setMensajeUsuario(String mensajeUsuario) {
        this.mensajeUsuario = mensajeUsuario;
    }

    public String getRespuestaIA() {
        return respuestaIA;
    }

    public void setRespuestaIA(String respuestaIA) {
        this.respuestaIA = respuestaIA;
    }

    public long getTiempoRespuestaMs() {
        return tiempoRespuestaMs;
    }

    public void setTiempoRespuestaMs(long tiempoRespuestaMs) {
        this.tiempoRespuestaMs = tiempoRespuestaMs;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getIdioma() {
        return idioma;
    }

    public void setIdioma(String idioma) {
        this.idioma = idioma;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getIpRemota() {
        return ipRemota;
    }

    public void setIpRemota(String ipRemota) {
        this.ipRemota = ipRemota;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getErrorDetalle() {
        return errorDetalle;
    }

    public void setErrorDetalle(String errorDetalle) {
        this.errorDetalle = errorDetalle;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Instant fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
