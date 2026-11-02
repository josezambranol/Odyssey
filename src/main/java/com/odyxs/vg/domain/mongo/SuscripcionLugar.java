package com.odyxs.vg.domain.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "suscripciones_lugares")
public class SuscripcionLugar {

    @Id
    private String id;

    @Indexed
    private Long usuarioId;

    @Indexed
    private String correo;

    private String nombreUsuario;

    @Indexed
    private Long lugarId;

    private String nombreLugar;

    private String categoriaLugar;

    private String ubicacionLugar;

    private String imagenLugar;

    private Instant fechaSuscripcion = Instant.now();

    private boolean recibirEventos = true;

    private boolean activo = true;

    public SuscripcionLugar() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String nombreUsuario) { this.nombreUsuario = nombreUsuario; }

    public Long getLugarId() { return lugarId; }
    public void setLugarId(Long lugarId) { this.lugarId = lugarId; }

    public String getNombreLugar() { return nombreLugar; }
    public void setNombreLugar(String nombreLugar) { this.nombreLugar = nombreLugar; }

    public String getCategoriaLugar() { return categoriaLugar; }
    public void setCategoriaLugar(String categoriaLugar) { this.categoriaLugar = categoriaLugar; }

    public String getUbicacionLugar() { return ubicacionLugar; }
    public void setUbicacionLugar(String ubicacionLugar) { this.ubicacionLugar = ubicacionLugar; }

    public String getImagenLugar() { return imagenLugar; }
    public void setImagenLugar(String imagenLugar) { this.imagenLugar = imagenLugar; }

    public Instant getFechaSuscripcion() { return fechaSuscripcion; }
    public void setFechaSuscripcion(Instant fechaSuscripcion) { this.fechaSuscripcion = fechaSuscripcion; }

    public boolean isRecibirEventos() { return recibirEventos; }
    public void setRecibirEventos(boolean recibirEventos) { this.recibirEventos = recibirEventos; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
