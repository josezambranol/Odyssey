package com.odyxs.vg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    public static final String VERSION_POLITICA_ACTUAL = "Ley1581-v2026.4";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String contrasena;

    @Column(name = "fecha_nacimiento")
    private String fechaNacimiento;

    @Column(name = "country")
    private String country;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Rol rol = Rol.USUARIO;

    /**
     * Prueba electrónica de autorización de tratamiento de datos personales
     * exigida por el Artículo 8 del Decreto 1377 de 2013 y Ley 1581 de 2012.
     */
    @Column(name = "fecha_consentimiento")
    private LocalDateTime fechaConsentimiento;

    @Column(name = "version_politica", length = 32)
    private String versionPolitica;

    public enum Rol {
        ADMIN, USUARIO
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }

    public String getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(String fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public LocalDateTime getFechaConsentimiento() { return fechaConsentimiento; }
    public void setFechaConsentimiento(LocalDateTime fechaConsentimiento) { this.fechaConsentimiento = fechaConsentimiento; }

    public String getVersionPolitica() { return versionPolitica; }
    public void setVersionPolitica(String versionPolitica) { this.versionPolitica = versionPolitica; }
}
