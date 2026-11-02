package com.odyxs.vg.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class GenerarItinerarioRequest {

    @Min(value = 1, message = "El itinerario debe ser de mínimo 1 día")
    @Max(value = 7, message = "El itinerario debe ser de máximo 7 días")
    private int dias = 3;

    @NotBlank(message = "El estilo de viaje es obligatorio (ej. PAREJA, FAMILIA, MOCHILERO, CULTURAL, FIESTA)")
    private String estiloViaje = "CULTURAL";

    private String presupuesto = "MEDIO"; // ECONOMICO, MEDIO, LUJO

    private Long usuarioId;

    private String usuarioEmail;

    private String preferenciasExtra;

    public int getDias() { return dias; }
    public void setDias(int dias) { this.dias = dias; }

    public String getEstiloViaje() { return estiloViaje; }
    public void setEstiloViaje(String estiloViaje) { this.estiloViaje = estiloViaje; }

    public String getPresupuesto() { return presupuesto; }
    public void setPresupuesto(String presupuesto) { this.presupuesto = presupuesto; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getPreferenciasExtra() { return preferenciasExtra; }
    public void setPreferenciasExtra(String preferenciasExtra) { this.preferenciasExtra = preferenciasExtra; }
}
