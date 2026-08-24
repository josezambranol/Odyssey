package com.odyxs.vg.controller;

import com.odyxs.vg.entity.*;
import com.odyxs.vg.repository.*;
import com.odyxs.vg.service.EventoSyncService;
import com.odyxs.vg.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired private ChatbotRespuestaRepository chatbotRepo;
    @Autowired private FileStorageService   fileStorageService;
    @Autowired private EventoSyncService    eventoSyncService;

    // ── Eventos (Web) ─────────────────────────────────────────

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

    @GetMapping("/eventos/sincronizar")
    public String sincronizarEventosWeb(RedirectAttributes redirectAttributes) {
        Map<String, Object> res = eventoSyncService.sincronizarEventosCartagena();
        redirectAttributes.addFlashAttribute("mensajeExito", "Cartelera cultural actualizada. Nuevos eventos agregados: " + res.get("nuevosEventos"));
        return "redirect:/eventos";
    }

    // ── API REST: Eventos en Tiempo Real (JSON) ───────────────

    @GetMapping("/api/eventos")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiObtenerEventos() {
        Map<String, List<Evento>> clasificados = eventoSyncService.obtenerEventosClasificados();
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "success");
        resp.put("ciudad", "Cartagena de Indias");
        resp.put("fechaServidor", LocalDate.now().toString());
        resp.put("hoy", clasificados.get("hoy"));
        resp.put("proximos", clasificados.get("proximos"));
        resp.put("finalizados", clasificados.get("finalizados"));
        resp.put("totalActivos", clasificados.get("hoy").size() + clasificados.get("proximos").size());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/api/eventos/sincronizar")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> apiSincronizarEventos() {
        Map<String, Object> resultado = eventoSyncService.sincronizarEventosCartagena();
        return ResponseEntity.ok(resultado);
    }

    // ── Proponer evento (usuarios) ────────────────────────────

    @GetMapping("/proponer-evento")
    public String proponerEventoForm() {
        return "proponer-evento";
    }

    @PostMapping("/proponer-evento")
    public String proponerEvento(@RequestParam String nombre,
                                 @RequestParam(required = false) String descripcion,
                                 @RequestParam String fecha,
                                 @RequestParam(required = false) String lugar,
                                 @RequestParam(required = false) MultipartFile imagen,
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

    // ── Chatbot ───────────────────────────────────────────────

    @GetMapping("/chatbot")
    public String chatbotPage() {
        return "chatbot";
    }

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private static final String ODYSSEY_SYSTEM_ES =
        "Eres Odyssey, asistente turístico de Cartagena de Indias. " +
        "Responde SIEMPRE en español, de forma amigable y concisa (máx. 3 párrafos). " +
        "Eres experto en: playas (Bocagrande, Barú, Islas del Rosario, Playa Blanca), " +
        "historia colonial (Castillo de San Felipe, murallas, Getsemaní), " +
        "transporte (taxis, Transcaribe, lanchas, aeropuerto Rafael Núñez), " +
        "hoteles y hospedaje, gastronomía caribeña (ceviche, patacón, arepas de huevo, langosta), " +
        "festivales (Hay Festival enero, FICCI marzo, Fiestas Independencia 11 de noviembre, Concurso Nacional de Belleza), " +
        "actividades (buceo, snorkel, chiva rumbera, kayak, tours en velero). " +
        "Usa emojis con moderación.";

    private static final String ODYSSEY_SYSTEM_EN =
        "You are Odyssey, the Cartagena tourist assistant for Cartagena de Indias, Colombia. " +
        "ALWAYS respond in English, in a friendly and concise way (max. 3 paragraphs). " +
        "You are an expert in: beaches (Bocagrande, Barú, Islas del Rosario, Playa Blanca), " +
        "colonial history (Castillo de San Felipe, city walls, Getsemaní neighborhood), " +
        "transport (taxis, Transcaribe bus, boats, Rafael Núñez Airport), " +
        "hotels and accommodation, Caribbean gastronomy (ceviche, patacón, arepas de huevo, lobster), " +
        "festivals (Hay Festival in January, FICCI in March, Independence Day November 11, National Beauty Pageant), " +
        "activities (diving, snorkeling, chiva rumbera, kayak, sailboat tours). " +
        "Use emojis in moderation.";

    @PostMapping("/chatbot/respuesta")
    @ResponseBody
    public ResponseEntity<Map<String, String>> responderChatbot(@RequestBody Map<String, String> body) {
        String mensaje = body.getOrDefault("mensaje", "").trim();
        if (mensaje.isBlank()) {
            return ResponseEntity.ok(Map.of("respuesta", "¿En qué puedo ayudarte? 😊"));
        }

        Locale locale = LocaleContextHolder.getLocale();
        boolean enIngles = "en".equalsIgnoreCase(locale.getLanguage());
        String systemPrompt = enIngles ? ODYSSEY_SYSTEM_EN : ODYSSEY_SYSTEM_ES;

        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                String respIA = llamarGeminiAPI(mensaje, systemPrompt);
                return ResponseEntity.ok(Map.of("respuesta", respIA));
            } catch (Exception ex) {
                System.err.println("[ODYSSEY] Error Gemini API: " + ex.getMessage());
            }
        }

        String clave = detectarClave(mensaje.toLowerCase());
        Optional<ChatbotRespuesta> opt = chatbotRepo.findByClave(clave);
        String respuesta = opt.isPresent() ? opt.get().getRespuesta()
            : (enIngles
                ? "🤔 Try asking me about: weather, beaches, transport, hotels, history, activities, events or gastronomy."
                : "🤔 Prueba preguntarme sobre: clima, playas, transporte, hoteles, historia, actividades, eventos o gastronomía.");
        return ResponseEntity.ok(Map.of("respuesta", respuesta));
    }

    private String llamarGeminiAPI(String mensaje, String systemPrompt) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        Map<String, Object> systemPart = Map.of("text", systemPrompt);
        Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));

        Map<String, Object> userPart = Map.of("text", mensaje);
        Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(userPart));

        Map<String, Object> generationConfig = Map.of("maxOutputTokens", 600, "temperature", 0.7);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);

        String requestJson = mapper.writeValueAsString(payload);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(url))
            .timeout(java.time.Duration.ofSeconds(30))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                requestJson, java.nio.charset.StandardCharsets.UTF_8))
            .build();

        java.net.http.HttpResponse<String> resp = client.send(
            request, java.net.http.HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            System.err.println("[ODYSSEY] Gemini body: " + resp.body());
            throw new RuntimeException("Gemini API HTTP " + resp.statusCode());
        }

        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(resp.body());
        return root.path("candidates").get(0)
                   .path("content").path("parts").get(0)
                   .path("text").asText();
    }

    private String detectarClave(String msg) {
        if (msg.matches("hola|buenos días|buenas|hi|hey|saludos|hello|good morning")) return "saludo";
        if (msg.contains("clima") || msg.contains("lluvi") || msg.contains("temperatur") || msg.contains("cuando visit")
            || msg.contains("weather") || msg.contains("rain") || msg.contains("when to visit")) return "clima";
        if (msg.contains("playa") || msg.contains("baru") || msg.contains("rosario") || msg.contains("bocagrande")
            || msg.contains("beach") || msg.contains("island")) return "playas";
        if (msg.contains("transport") || msg.contains("mover") || msg.contains("llegar") || msg.contains("bus") || msg.contains("taxi") || msg.contains("yate")
            || msg.contains("get around") || msg.contains("boat") || msg.contains("airport")) return "transporte";
        if (msg.contains("hotel") || msg.contains("aloj") || msg.contains("hostal") || msg.contains("donde dormir")
            || msg.contains("stay") || msg.contains("sleep") || msg.contains("accommodation")) return "hoteles";
        if (msg.contains("histor") || msg.contains("coloni") || msg.contains("murall") || msg.contains("castill") || msg.contains("fundaci")
            || msg.contains("wall") || msg.contains("castle") || msg.contains("fort")) return "historico";
        if (msg.contains("actividad") || msg.contains("hacer") || msg.contains("tour") || msg.contains("buceo") || msg.contains("kayak")
            || msg.contains("activity") || msg.contains("diving") || msg.contains("things to do")) return "actividades";
        if (msg.contains("evento") || msg.contains("festival") || msg.contains("hay festival") || msg.contains("ficci")
            || msg.contains("event") || msg.contains("festival")) return "eventos";
        if (msg.contains("costo") || msg.contains("precio") || msg.contains("caro") || msg.contains("barato") || msg.contains("dinero")
            || msg.contains("cost") || msg.contains("price") || msg.contains("budget") || msg.contains("cheap")) return "costo";
        if (msg.contains("comer") || msg.contains("comida") || msg.contains("gastronom") || msg.contains("restaur") || msg.contains("ceviche")
            || msg.contains("food") || msg.contains("eat") || msg.contains("restaurant") || msg.contains("gastronomy")) return "gastronomia";
        if (msg.contains("adios") || msg.contains("gracias") || msg.contains("bye") || msg.contains("chao")
            || msg.contains("goodbye") || msg.contains("thank")) return "despedida";
        return "saludo";
    }
}