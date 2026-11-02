package com.odyxs.vg.controller;

import com.odyxs.vg.entity.*;
import com.odyxs.vg.repository.*;
import com.odyxs.vg.service.EventoSyncService;
import com.odyxs.vg.service.FileStorageService;
import com.odyxs.vg.service.GeminiAiService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Controller
public class TurismoController {

    @Autowired private EventoRepository     eventoRepo;
    @Autowired private ActividadRepository  actividadRepo;
    @Autowired private FileStorageService   fileStorageService;
    @Autowired private EventoSyncService    eventoSyncService;
    @Autowired private GeminiAiService      geminiAiService;

    // ── Eventos (Web) ─────────────────────────────────────────────

    @GetMapping("/eventos")
    public String eventos(@RequestParam(required = false) String desde,
                          @RequestParam(required = false) String synced,
                          Model model) {
        LocalDate hoy = LocalDate.now();

        if (desde != null && !desde.isBlank()) {
            try {
                LocalDate fechaDesde = LocalDate.parse(desde.trim());
                List<Evento> filtrados = eventoRepo.findByActivoTrueAndFechaGreaterThanEqualOrderByFechaAsc(fechaDesde);
                model.addAttribute("eventosFiltrados", filtrados);
                model.addAttribute("desde", desde);
            } catch (DateTimeParseException e) {
                model.addAttribute("error", "Formato de fecha no válido.");
            }
        }

        Map<String, List<Evento>> clasificados = eventoSyncService.obtenerEventosClasificados();
        model.addAttribute("eventosHoy", clasificados.get("hoy"));
        model.addAttribute("eventosProximos", clasificados.get("proximos"));
        model.addAttribute("eventosFinalizados", clasificados.get("finalizados"));
        model.addAttribute("fechaActual", hoy);

        if ("true".equals(synced)) {
            model.addAttribute("mensajeExito", "¡Cartelera de eventos sincronizada en tiempo real con éxito!");
        }

        return "eventos";
    }

    // ── Actividades (Web) ─────────────────────────────────────────

    @GetMapping("/actividades")
    public String actividades(@RequestParam(required = false, name = "cat") String cat,
                              @RequestParam(required = false, name = "categoria") String categoria,
                              Model model) {
        String filtro = (cat != null && !cat.isBlank()) ? cat : categoria;
        List<Actividad> lista;
        if (filtro != null && !filtro.isBlank()) {
            try {
                Actividad.CategoriaActividad catEnum = Actividad.CategoriaActividad.valueOf(filtro.trim().toUpperCase());
                lista = actividadRepo.findByCategoriaAndEstado(catEnum, Actividad.Estado.APROBADO);
                model.addAttribute("catActiva", catEnum.name());
                model.addAttribute("categoriaSeleccionada", catEnum.name());
            } catch (IllegalArgumentException e) {
                lista = actividadRepo.findByEstado(Actividad.Estado.APROBADO);
            }
        } else {
            lista = actividadRepo.findByEstado(Actividad.Estado.APROBADO);
        }
        model.addAttribute("actividades", lista);
        model.addAttribute("categorias", Actividad.CategoriaActividad.values());
        return "actividades";
    }

    // ── Proponer Actividad (Usuario) ──────────────────────────────

    @GetMapping("/proponer-actividad")
    public String proponerActividadForm(Model model) {
        model.addAttribute("categorias", Actividad.CategoriaActividad.values());
        return "proponer-actividad";
    }

    @PostMapping("/proponer-actividad")
    public String guardarPropuestaActividad(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) String duracion,
            @RequestParam(required = false) String precioAprox,
            @RequestParam String categoria,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            RedirectAttributes redirectAttributes) {

        try {
            Actividad.CategoriaActividad cat = Actividad.CategoriaActividad.valueOf(categoria.toUpperCase());
            Actividad a = new Actividad();
            a.setNombre(nombre.trim());
            a.setDescripcion(descripcion != null ? descripcion.trim() : null);
            a.setDuracion(duracion != null ? duracion.trim() : null);
            a.setPrecioAprox(precioAprox != null ? precioAprox.trim() : null);
            a.setCategoria(cat);
            a.setEsOficial(false);
            a.setEstado(Actividad.Estado.PENDIENTE);
            a.setImagenUrl(fileStorageService.guardarImagen(imagen, "actividades"));

            actividadRepo.save(a);
            redirectAttributes.addFlashAttribute("mensaje",
                "¡Tu propuesta de actividad fue enviada! Será revisada por el equipo Odyssey.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "Categoría no válida.");
            return "redirect:/proponer-actividad";
        }
        return "redirect:/actividades";
    }

    // ── Proponer Evento (Usuario) ─────────────────────────────────

    @GetMapping("/proponer-evento")
    public String proponerEventoForm() {
        return "proponer-evento";
    }

    @PostMapping("/proponer-evento")
    public String guardarPropuestaEvento(
            @RequestParam String nombre,
            @RequestParam(required = false) String descripcion,
            @RequestParam String fecha,
            @RequestParam(required = false) String lugar,
            @RequestParam(value = "imagen", required = false) MultipartFile imagen,
            Model model) {

        try {
            Evento e = new Evento();
            e.setNombre(nombre.trim());
            e.setDescripcion(descripcion != null ? descripcion.trim() : null);
            e.setFecha(LocalDate.parse(fecha.trim()));
            e.setLugar(lugar != null ? lugar.trim() : null);
            e.setImagenUrl(fileStorageService.guardarImagen(imagen, "eventos"));
            e.setActivo(false); // pendiente de aprobación admin

            eventoRepo.save(e);
            model.addAttribute("mensaje", "¡Propuesta de evento enviada! Será revisada por el equipo Odyssey.");
        } catch (DateTimeParseException ex) {
            model.addAttribute("error", "Formato de fecha inválido. Utilice el formato AAAA-MM-DD.");
        }
        return "proponer-evento";
    }

    // ── Transporte ────────────────────────────────────────────

    @GetMapping("/transporte")
    public String transporte() {
        return "transporte";
    }

    // ── Guía completa (info, clima, top5, cuándo visitar) ─────

    @GetMapping("/guia")
    public String guia(Model model) {
        return "guia";
    }

    // ── Mapa Interactivo ─────────────────────────────────────

    @GetMapping("/mapa")
    public String mapa() {
        return "mapa";
    }

    // ── Chatbot ODYSSEY con Google Gemini & Telemetría NoSQL ──

    @GetMapping("/chatbot")
    public String chatbotPage(Model model) {
        model.addAttribute("geminiActivo", geminiAiService.tieneGeminiKeyConfigurada());
        return "chatbot";
    }

    @PostMapping("/chatbot/respuesta")
    @ResponseBody
    public ResponseEntity<Map<String, String>> responderChatbot(@RequestBody Map<String, String> body,
                                                                 HttpServletRequest request) {
        String mensaje = body.getOrDefault("mensaje", "").trim();
        Locale locale = LocaleContextHolder.getLocale();
        boolean enIngles = "en".equalsIgnoreCase(locale.getLanguage());
        String clientIp = request.getRemoteAddr();

        String respuesta = geminiAiService.responder(mensaje, enIngles, clientIp);
        return ResponseEntity.ok(Map.of(
                "respuesta", respuesta,
                "motor", geminiAiService.tieneGeminiKeyConfigurada() ? "Google Gemini 2.5 Flash + RAG" : "Odyssey Cloud AI + RAG Cartagena"
        ));
    }

    @PostMapping("/chatbot/config-key")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> configurarGeminiKey(@RequestBody Map<String, String> body) {
        String key = body.getOrDefault("apiKey", "").trim();
        if (key.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("exito", false, "mensaje", "La API Key no puede estar vacía."));
        }
        geminiAiService.actualizarApiKey(key);
        return ResponseEntity.ok(Map.of(
                "exito", true,
                "mensaje", "Google Gemini API Key activada en tiempo real para esta sesión."
        ));
    }
}
