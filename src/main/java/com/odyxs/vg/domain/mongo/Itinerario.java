package com.odyxs.vg.domain.mongo;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document(collection = "itinerarios")
public class Itinerario {

    @Id
    private String id;

    @Indexed(unique = true)
    private String codigo;

    private String titulo;

    @Indexed
    private String estiloViaje; // PAREJA, FAMILIA, MOCHILERO, CULTURAL, FIESTA

    private int dias;

    private String presupuesto; // ECONOMICO, MEDIO, LUJO

    @Indexed
    private Long usuarioId;

    private String usuarioEmail;

    private List<DiaItinerario> planDias = new ArrayList<>();

    private Map<String, Object> recomendacionesGenerales = new HashMap<>();

    @CreatedDate
    @Indexed
    private Instant fechaCreacion;

    public Itinerario() {
        this.fechaCreacion = Instant.now();
    }

    public static class DiaItinerario {
        private int numeroDia;
        private String tituloDia;
        private String manana;
        private String tarde;
        private String noche;
        private List<String> lugaresSugeridos = new ArrayList<>();
        private String costoAproximadoCop;

        public DiaItinerario() {}

        public DiaItinerario(int numeroDia, String tituloDia, String manana, String tarde, String noche,
                             List<String> lugaresSugeridos, String costoAproximadoCop) {
            this.numeroDia = numeroDia;
            this.tituloDia = tituloDia;
            this.manana = manana;
            this.tarde = tarde;
            this.noche = noche;
            this.lugaresSugeridos = lugaresSugeridos;
            this.costoAproximadoCop = costoAproximadoCop;
        }

        public int getNumeroDia() { return numeroDia; }
        public void setNumeroDia(int numeroDia) { this.numeroDia = numeroDia; }

        public String getTituloDia() { return tituloDia; }
        public void setTituloDia(String tituloDia) { this.tituloDia = tituloDia; }

        public String getManana() { return manana; }
        public void setManana(String manana) { this.manana = manana; }

        public String getTarde() { return tarde; }
        public void setTarde(String tarde) { this.tarde = tarde; }

        public String getNoche() { return noche; }
        public void setNoche(String noche) { this.noche = noche; }

        public List<String> getLugaresSugeridos() { return lugaresSugeridos; }
        public void setLugaresSugeridos(List<String> lugaresSugeridos) { this.lugaresSugeridos = lugaresSugeridos; }

        public String getCostoAproximadoCop() { return costoAproximadoCop; }
        public void setCostoAproximadoCop(String costoAproximadoCop) { this.costoAproximadoCop = costoAproximadoCop; }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getEstiloViaje() { return estiloViaje; }
    public void setEstiloViaje(String estiloViaje) { this.estiloViaje = estiloViaje; }

    public int getDias() { return dias; }
    public void setDias(int dias) { this.dias = dias; }

    public String getPresupuesto() { return presupuesto; }
    public void setPresupuesto(String presupuesto) { this.presupuesto = presupuesto; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public List<DiaItinerario> getPlanDias() { return planDias; }
    public void setPlanDias(List<DiaItinerario> planDias) { this.planDias = planDias; }

    public Map<String, Object> getRecomendacionesGenerales() { return recomendacionesGenerales; }
    public void setRecomendacionesGenerales(Map<String, Object> recomendacionesGenerales) { this.recomendacionesGenerales = recomendacionesGenerales; }

    public Instant getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Instant fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
