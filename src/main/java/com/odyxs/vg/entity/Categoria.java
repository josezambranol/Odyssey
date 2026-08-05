package com.odyxs.vg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "categorias")
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String nombre;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    /**
     * Devuelve una clave segura para i18n: sin espacios (→ _) ni tildes.
     * Ejemplo: "SITIOS HISTÓRICOS" → "SITIOS_HISTORICOS"
     * Se usa en las vistas como #{'cat.' + cat.nombreClave}
     */
    public String getNombreClave() {
        if (nombre == null) return "";
        return java.text.Normalizer
            .normalize(nombre, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .replaceAll("[^A-Z0-9]", "_");
    }
}
