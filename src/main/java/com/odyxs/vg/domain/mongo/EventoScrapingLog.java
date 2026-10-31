package com.odyxs.vg.domain.mongo;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Document(collection = "evento_scraping_logs")
public class EventoScrapingLog {

    @Id
    private String id;

    @Indexed
    private String fuenteUrl;

    private String nombreFuente;

    private int eventosExtraidos;

    private int eventosNuevos;

    private int eventosDuplicados;

    private boolean exito = true;

    private String errorMensaje;

    private long latenciaMs;

    private List<Map<String, String>> eventosMuestra = new ArrayList<>();

    @CreatedDate
    @Indexed
    private Instant fechaScraping;

    public EventoScrapingLog() {
        this.fechaScraping = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFuenteUrl() {
        return fuenteUrl;
    }

    public void setFuenteUrl(String fuenteUrl) {
        this.fuenteUrl = fuenteUrl;
    }

    public String getNombreFuente() {
        return nombreFuente;
    }

    public void setNombreFuente(String nombreFuente) {
        this.nombreFuente = nombreFuente;
    }

    public int getEventosExtraidos() {
        return eventosExtraidos;
    }

    public void setEventosExtraidos(int eventosExtraidos) {
        this.eventosExtraidos = eventosExtraidos;
    }

    public int getEventosNuevos() {
        return eventosNuevos;
    }

    public void setEventosNuevos(int eventosNuevos) {
        this.eventosNuevos = eventosNuevos;
    }

    public int getEventosDuplicados() {
        return eventosDuplicados;
    }

    public void setEventosDuplicados(int eventosDuplicados) {
        this.eventosDuplicados = eventosDuplicados;
    }

    public boolean isExito() {
        return exito;
    }

    public void setExito(boolean exito) {
        this.exito = exito;
    }

    public String getErrorMensaje() {
        return errorMensaje;
    }

    public void setErrorMensaje(String errorMensaje) {
        this.errorMensaje = errorMensaje;
    }

    public long getLatenciaMs() {
        return latenciaMs;
    }

    public void setLatenciaMs(long latenciaMs) {
        this.latenciaMs = latenciaMs;
    }

    public List<Map<String, String>> getEventosMuestra() {
        return eventosMuestra;
    }

    public void setEventosMuestra(List<Map<String, String>> eventosMuestra) {
        this.eventosMuestra = eventosMuestra;
    }

    public Instant getFechaScraping() {
        return fechaScraping;
    }

    public void setFechaScraping(Instant fechaScraping) {
        this.fechaScraping = fechaScraping;
    }
}
