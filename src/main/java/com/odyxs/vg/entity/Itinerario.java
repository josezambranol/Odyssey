package com.odyxs.vg.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odyxs.vg.common.JsonMapConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "itinerarios", indexes = {
    @Index(name = "idx_itin_usuario", columnList = "usuarioId"),
    @Index(name = "idx_itin_estilo", columnList = "estiloViaje")
})
public class Itinerario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(unique = true, nullable = false, length = 64)
    private String codigo;

    @Column(length = 255)
    private String titulo;

    @Column(length = 64)
    private String estiloViaje;

    private int dias;

    @Column(length = 32)
    private String presupuesto;

    private Long usuarioId;

    @Column(length = 150)
    private String usuarioEmail;

    @Convert(converter = DiaListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<DiaItinerario> planDias = new ArrayList<>();

    @Convert(converter = JsonMapConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, Object> recomendacionesGenerales = new HashMap<>();

    private Instant fechaCreacion;

    public Itinerario() {
        this.fechaCreacion = Instant.now();
    }

    @Converter
    public static class DiaListConverter implements AttributeConverter<List<DiaItinerario>, String> {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        @Override
        public String convertToDatabaseColumn(List<DiaItinerario> attribute) {
            if (attribute == null || attribute.isEmpty()) return "[]";
            try {
                return MAPPER.writeValueAsString(attribute);
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Error serializando planDias", e);
            }
        }

        @Override
        public List<DiaItinerario> convertToEntityAttribute(String dbData) {
            if (dbData == null || dbData.isBlank()) return new ArrayList<>();
            try {
                return MAPPER.readValue(dbData, new TypeReference<>() {});
            } catch (JsonProcessingException e) {
                return new ArrayList<>();
            }
        }
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
