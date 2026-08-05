package com.odyxs.vg.controller;

import com.odyxs.vg.entity.*;
import com.odyxs.vg.repository.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

@Controller
public class TurismoController {

    @Autowired private EventoRepository     eventoRepo;
    @Autowired private ActividadRepository  actividadRepo;
    @Autowired private ChatbotRespuestaRepository chatbotRepo;

    private static final String EVENTOS_DIR =
        System.getProperty("user.home") + "/odyxs-uploads/eventos/";

    // ── Eventos ───────────────────────────────────────────────

    @GetMapping("/eventos")
    public String eventos(
            @RequestParam(required = false) String desde,
            Model model, HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        List<Evento> lista;
        if (desde != null && !desde.isBlank()) {
            lista = eventoRepo.findByActivoTrueAndFechaGreaterThanEqualOrderByFechaAsc(
                        LocalDate.parse(desde));
        } else {
            lista = eventoRepo.findByActivoTrueOrderByFechaAsc();
        }
        model.addAttribute("eventos", lista);
        model.addAttribute("desde", desde);
        return "eventos";
    }

    // Gestión de eventos: ver AdminController



    private String guardarImagenEvento(MultipartFile imagen) {
        if (imagen == null || imagen.isEmpty()) return null;
        String tipo = imagen.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) return null;
        try {
            Files.createDirectories(Paths.get(EVENTOS_DIR));
            String ext = "";
            String original = imagen.getOriginalFilename();
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf("."));
            } else {
                // Inferir extensión del content-type
                ext = tipo.equals("image/png") ? ".png"
                    : tipo.equals("image/webp") ? ".webp"
                    : tipo.equals("image/gif") ? ".gif"
                    : ".jpg";
            }
            String nombreArchivo = System.currentTimeMillis() + ext;
            Path destino = Paths.get(EVENTOS_DIR + nombreArchivo);
            Files.copy(imagen.getInputStream(), destino, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/eventos/" + nombreArchivo;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ── Proponer evento (usuarios) ────────────────────────────

    @GetMapping("/proponer-evento")
    public String proponerEventoForm(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        return "proponer-evento";
    }

    @PostMapping("/proponer-evento")
    public String proponerEvento(@RequestParam String nombre,
                                 @RequestParam(required = false) String descripcion,
                                 @RequestParam String fecha,
                                 @RequestParam(required = false) String lugar,
                                 @RequestParam(required = false) MultipartFile imagen,
                                 org.springframework.ui.Model model,
                                 HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";

        Evento e = new Evento();
        e.setNombre(nombre);
        e.setDescripcion(descripcion);
        e.setFecha(LocalDate.parse(fecha));
        e.setLugar(lugar);
        e.setImagenUrl(guardarImagenEvento(imagen));
        e.setActivo(false); // pendiente de aprobación admin

        eventoRepo.save(e);

        model.addAttribute("mensaje", "¡Propuesta de evento enviada! Será revisada por el equipo ODYXS.");
        return "proponer-evento";
    }

    // ── Transporte ────────────────────────────────────────────
    // Datos estáticos definidos en i18n/messages*.properties — no requiere BD

    @GetMapping("/transporte")
    public String transporte(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        return "transporte";
    }

    // ── Actividades ── gestionado por ActividadController ───────

    // ── Guía completa (info, clima, top5, cuándo visitar) ─────

    @GetMapping("/guia")
    public String guia(HttpSession session, Model model) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        return "guia";
    }

    // ── Mapa Interactivo ─────────────────────────────────────

    @GetMapping("/mapa")
    public String mapa(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        return "mapa";
    }

    // ── Chatbot ───────────────────────────────────────────────

    @GetMapping("/chatbot")
    public String chatbotPage(HttpSession session) {
        if (session.getAttribute("usuarioId") == null) return "redirect:/login";
        return "chatbot";
    }

    // ══════════════════════════════════════════════════════════
    // Chatbot ODYX — Google Gemini (gemini-2.5-flash, GRATUITO)
    // Bilingüe: detecta el idioma de la sesión (ES / EN)
    //
    // Cómo obtener tu API key GRATIS:
    //   1. Ve a https://aistudio.google.com/app/apikey
    //   2. Inicia sesión con tu cuenta Google
    //   3. Clic en "Create API key" → copia la key
    //   4. Pégala en application.properties:
    //        gemini.api.key=AIzaSy...
    // ══════════════════════════════════════════════════════════

    @org.springframework.beans.factory.annotation.Value("${gemini.api.key:}")
    private String geminiApiKey;

    // System prompt en ESPAÑOL
    private static final String ODYX_SYSTEM_ES =
        "Eres ODYX, asistente turístico de ODYXS para Cartagena de Indias. " +
        "Responde SIEMPRE en español, de forma amigable y concisa (máx. 3 párrafos). " +
        "Eres experto en: playas (Bocagrande, Barú, Islas del Rosario, Playa Blanca), " +
        "historia colonial (Castillo de San Felipe, murallas, Getsemaní), " +
        "transporte (taxis, Transcaribe, lanchas, aeropuerto Rafael Núñez), " +
        "hoteles y hospedaje, gastronomía caribeña (ceviche, patacón, arepas de huevo, langosta), " +
        "festivales (Hay Festival enero, FICCI marzo, Fiestas Independencia 11 de noviembre, Concurso Nacional de Belleza), " +
        "actividades (buceo, snorkel, chiva rumbera, kayak, tours en velero). " +
        "Usa emojis con moderación.";

    // System prompt in ENGLISH
    private static final String ODYX_SYSTEM_EN =
        "You are ODYX, the ODYXS tourist assistant for Cartagena de Indias, Colombia. " +
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
    public ResponseEntity<Map<String, String>> responderChatbot(
            @RequestBody Map<String, String> body,
            HttpSession session) {

        String mensaje = body.getOrDefault("mensaje", "").trim();
        if (mensaje.isBlank()) {
            return ResponseEntity.ok(Map.of("respuesta", "¿En qué puedo ayudarte? 😊"));
        }

        // Detectar idioma actual via CookieLocaleResolver (WebConfig)
        java.util.Locale locale = LocaleContextHolder.getLocale();
        boolean enIngles = "en".equalsIgnoreCase(locale.getLanguage());
        String systemPrompt = enIngles ? ODYX_SYSTEM_EN : ODYX_SYSTEM_ES;

        // ── Intentar Gemini AI ───────────────────────────────────
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                String respIA = llamarGeminiAPI(mensaje, systemPrompt);
                return ResponseEntity.ok(Map.of("respuesta", respIA));
            } catch (Exception ex) {
                System.err.println("[ODYX] Error Gemini API: " + ex.getMessage());
            }
        }

        // ── Fallback BD ──────────────────────────────────────────
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

        java.util.Map<String, Object> systemPart = java.util.Map.of("text", systemPrompt);
        java.util.Map<String, Object> systemInstruction = java.util.Map.of(
            "parts", java.util.List.of(systemPart)
        );

        java.util.Map<String, Object> userPart = java.util.Map.of("text", mensaje);
        java.util.Map<String, Object> userContent = java.util.Map.of(
            "role", "user",
            "parts", java.util.List.of(userPart)
        );

        java.util.Map<String, Object> generationConfig = java.util.Map.of(
            "maxOutputTokens", 600,
            "temperature", 0.7
        );

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", java.util.List.of(userContent));
        payload.put("generationConfig", generationConfig);

        String requestJson = mapper.writeValueAsString(payload);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

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
            System.err.println("[ODYX] Gemini body: " + resp.body());
            throw new RuntimeException("Gemini API HTTP " + resp.statusCode());
        }

        // Parsear: candidates[0].content.parts[0].text
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

    private boolean esAdmin(HttpSession session) {
        Object rol = session.getAttribute("usuarioRol");
        return rol != null && rol.toString().equals("ADMIN");
    }
}
