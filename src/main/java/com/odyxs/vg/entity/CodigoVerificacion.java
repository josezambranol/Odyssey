package com.odyxs.vg.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "codigos_verificacion", indexes = {
    @Index(name = "idx_codigo_correo", columnList = "correo")
})
public class CodigoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(nullable = false, length = 16)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TipoCodigo tipo;

    private Instant fechaCreacion = Instant.now();

    private Instant expiraEn;

    private boolean usado = false;

    private boolean enviadoPorSmtp = false;

    public enum TipoCodigo {
        RECUPERAR_PASSWORD,
        LOGIN_OTP,
        VERIFICAR_CUENTA
    }

    public CodigoVerificacion() {}

    public CodigoVerificacion(String correo, String codigo, TipoCodigo tipo, Instant expiraEn) {
        this.correo = correo;
        this.codigo = codigo;
        this.tipo = tipo;
        this.expiraEn = expiraEn;
        this.fechaCreacion = Instant.now();
    }

    public boolean estaExpirado() {
        return expiraEn != null && Instant.now().isAfter(expiraEn);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public TipoCodigo getTipo() { return tipo; }
    public void setTipo(TipoCodigo tipo) { this.tipo = tipo; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Instant getExpiraEn() { return expiraEn; }
    public void setExpiraEn(Instant expiraEn) { this.expiraEn = expiraEn; }

    public boolean isUsado() { return usado; }
    public void setUsado(boolean usado) { this.usado = usado; }

    public boolean isEnviadoPorSmtp() { return enviadoPorSmtp; }
    public void setEnviadoPorSmtp(boolean enviadoPorSmtp) { this.enviadoPorSmtp = enviadoPorSmtp; }
}
