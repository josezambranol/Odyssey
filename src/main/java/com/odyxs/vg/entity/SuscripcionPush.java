package com.odyxs.vg.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Suscripción Web Push PWA (Estándar W3C Push API + Claves VAPID P-256).
 */
@Entity
@Table(
        name = "suscripciones_push",
        indexes = {
                @Index(name = "idx_push_lugar", columnList = "lugar_id"),
                @Index(name = "idx_push_usuario", columnList = "usuario_id")
        }
)
public class SuscripcionPush {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, length = 255)
    private String p256dh;

    @Column(name = "auth_secret", nullable = false, length = 128)
    private String auth;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "lugar_id")
    private Long lugarId;

    @Column(length = 8, nullable = false)
    private String idioma = "es";

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }

    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public Long getLugarId() { return lugarId; }
    public void setLugarId(Long lugarId) { this.lugarId = lugarId; }

    public String getIdioma() { return idioma; }
    public void setIdioma(String idioma) { this.idioma = idioma; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
}
