package com.odyxs.vg.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "suscripciones_lugares", indexes = {
    @Index(name = "idx_sub_usuario_lugar", columnList = "usuarioId, lugarId")
})
public class SuscripcionLugar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    private Long usuarioId;

    @Column(length = 150)
    private String correo;

    @Column(length = 120)
    private String nombreUsuario;

    private Long lugarId;

    @Column(length = 255)
    private String nombreLugar;

    @Column(length = 100)
    private String categoriaLugar;

    @Column(length = 255)
    private String ubicacionLugar;

    @Column(length = 500)
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
