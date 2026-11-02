package com.odyxs.vg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odyxs.vg.domain.mongo.ChatbotLog;
import com.odyxs.vg.entity.Actividad;
import com.odyxs.vg.entity.ChatbotRespuesta;
import com.odyxs.vg.entity.Evento;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.ActividadRepository;
import com.odyxs.vg.repository.ChatbotRespuestaRepository;
import com.odyxs.vg.repository.EventoRepository;
import com.odyxs.vg.repository.LugarRepository;
import com.odyxs.vg.repository.mongo.ChatbotLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired(required = false)
    private ChatbotLogRepository chatbotLogRepository;

    @Autowired(required = false)
    private ChatbotRespuestaRepository chatbotRespuestaRepository;

    @Autowired(required = false)
    private LugarRepository lugarRepository;

    @Autowired(required = false)
    private ActividadRepository actividadRepository;

    @Autowired(required = false)
    private EventoRepository eventoRepository;

    @Autowired(required = false)
    private RedisMetricsService redisMetricsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash"
    );

    private static final String BASE_SYSTEM_ES =
            "Eres ODYSSEY, el Concierge IA oficial de Cartagena de Indias, Colombia (Patrimonio UNESCO). " +
            "Responde SIEMPRE en español con tono cálido, experto y estructurado usando viñetas y **negritas**. " +
            "Datos verificados: Transcaribe $3.000 COP (T102 Aeropuerto-Centro, T103 Bocagrande). " +
            "Taxis usan tarifa oficial DATT sin taxímetro (Mínima ~$10.000, Aeropuerto-Centro ~$16.000, Aeropuerto-Bocagrande ~$24.000). " +
            "Lanchas a Islas del Rosario y Barú salen del Muelle de la Bodeguita ($70.000-$90.000 COP + impuesto). " +
            "Lugares icónicos: Castillo San Felipe, Torre del Reloj, Baluarte de Santo Domingo, Café del Mar, Getsemaní (Plaza de la Trinidad, Calle de la Sierpe, Celele), La Cevichería, Convento de la Popa, Playa Blanca, Cholón, La Boquilla.";

    private static final String BASE_SYSTEM_EN =
            "You are ODYSSEY, the official AI Concierge for Cartagena de Indias, Colombia (UNESCO World Heritage). " +
            "ALWAYS respond in English in a warm, expert, structured tone using bullet points and **bold** text. " +
            "Official facts: Transcaribe bus is $3,000 COP (Line T102 Airport-Center; T103 Bocagrande). " +
            "Yellow taxis use DATT flat rates (Airport-Center ~$16,000 COP). Boats to Rosario Islands depart from Muelle de la Bodeguita.";

    public synchronized void actualizarApiKey(String nuevaKey) {
        if (nuevaKey != null && !nuevaKey.isBlank()) {
            this.geminiApiKey = nuevaKey.trim();
            log.info("[ODYSSEY-AI] Google Gemini API Key actualizada dinámicamente.");
        }
    }

    public boolean tieneGeminiKeyConfigurada() {
        return geminiApiKey != null && !geminiApiKey.isBlank()
                && !geminiApiKey.contains("TU_KEY")
                && !geminiApiKey.contains("tu_api_key");
    }

    public static String normalizarTexto(String input) {
        if (input == null) return "";
        String norm = Normalizer.normalize(input, Normalizer.Form.NFD);
        return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").toLowerCase(Locale.ROOT);
    }

    public String responder(String mensajeRaw, boolean enIngles, String clientIp) {
        if (mensajeRaw == null || mensajeRaw.isBlank()) {
            return enIngles
                    ? "How can I help you explore Cartagena de Indias today? 🌊"
                    : "¿En qué puedo ayudarte a explorar Cartagena de Indias hoy? 🌊";
        }

        String mensaje = mensajeRaw.replace("[[", "").replace("]]", "").trim();

        if (redisMetricsService != null && !redisMetricsService.verificarLimiteRateChatbot(clientIp)) {
            return enIngles
                    ? "⚠️ Too many requests. Please wait a minute before asking again."
                    : "⚠️ Has enviado muchas preguntas en poco tiempo. Por favor espera un minuto.";
        }

        String categoriaDetectada = detectarCategoria(normalizarTexto(mensaje));
        String contextoCompacto = construirContextoCompactoRag(mensaje);
        String systemPrompt = (enIngles ? BASE_SYSTEM_EN : BASE_SYSTEM_ES)
                + (contextoCompacto.isBlank() ? "" : "\n" + contextoCompacto);

        long inicio = System.currentTimeMillis();
        String respuesta = null;
        boolean exito = false;
        String errorMsg = null;

        // NIVEL 1: Google Gemini API Oficial (si la API Key está configurada)
        if (tieneGeminiKeyConfigurada()) {
            try {
                respuesta = llamarGoogleGeminiMultiModelo(mensaje, systemPrompt);
                if (respuesta != null && !respuesta.isBlank()) {
                    exito = true;
                }
            } catch (Exception ex) {
                log.warn("[ODYSSEY-AI] Fallo en Gemini API directa: {}. Activando motor generativo secundario.", ex.getMessage());
                errorMsg = ex.getMessage();
            }
        }

        // NIVEL 2: Motor Generativo Cloud Zero-Config con RAG compacto de Cartagena
        if (!exito || respuesta == null || respuesta.isBlank()) {
            try {
                respuesta = llamarMotorGenerativoCloudRag(mensaje, systemPrompt);
                if (respuesta != null && !respuesta.isBlank()) {
                    exito = true;
                }
            } catch (Exception ex) {
                log.warn("[ODYSSEY-AI] Motor cloud secundario no disponible: {}. Usando motor RAG local.", ex.getMessage());
                if (errorMsg == null) errorMsg = ex.getMessage();
            }
        }

        // NIVEL 3: Motor RAG Local Inteligente + Síntesis Dinámica de Planes y Lugares
        if (!exito || respuesta == null || respuesta.isBlank()) {
            respuesta = sintetizarRespuestaRagLocal(mensaje, categoriaDetectada, enIngles);
        }

        long duracionMs = System.currentTimeMillis() - inicio;
        registrarLogChatbot(mensaje, respuesta, duracionMs, categoriaDetectada, clientIp, enIngles ? "en" : "es", exito, errorMsg);

        return respuesta;
    }

    /**
     * Construye un contexto RAG ultracompacto (< 700 caracteres) con los lugares más relevantes a la pregunta
     * para garantizar respuestas rápidas (< 4s) en los motores generativos.
     */
    private String construirContextoCompactoRag(String consulta) {
        try {
            if (lugarRepository == null) return "";
            String qNorm = normalizarTexto(consulta);
            List<String> tokens = Arrays.stream(qNorm.split("[^a-z0-9]+"))
                    .filter(w -> w.length() >= 4)
                    .toList();

            List<Lugar> aprobados = lugarRepository.findByEstado(Lugar.Estado.APROBADO);
            if (aprobados.isEmpty()) return "";

            List<Lugar> relevantes = aprobados.stream()
                    .sorted((a, b) -> Integer.compare(puntajeRelevancia(b, tokens, qNorm), puntajeRelevancia(a, tokens, qNorm)))
                    .limit(5)
                    .toList();

            return "Contexto BD Odyssey: " + relevantes.stream()
                    .map(l -> l.getNombre() + " (" + l.getUbicacion() + ")")
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            return "";
        }
    }

    private int puntajeRelevancia(Lugar l, List<String> tokens, String qNorm) {
        int score = 0;
        String nombreNorm = normalizarTexto(l.getNombre());
        String descNorm = normalizarTexto(l.getDescripcion());
        String ubiNorm = normalizarTexto(l.getUbicacion());
        if (qNorm.contains(nombreNorm)) score += 10;
        for (String t : tokens) {
            if (nombreNorm.contains(t)) score += 5;
            if (ubiNorm.contains(t)) score += 4;
            if (descNorm.contains(t)) score += 2;
        }
        return score;
    }

    private String llamarGoogleGeminiMultiModelo(String mensaje, String systemPrompt) throws Exception {
        Map<String, Object> systemPart = Map.of("text", systemPrompt);
        Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));

        Map<String, Object> userPart = Map.of("text", mensaje);
        Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(userPart));

        Map<String, Object> generationConfig = Map.of(
                "maxOutputTokens", 700,
                "temperature", 0.65
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);

        String requestJson = objectMapper.writeValueAsString(payload);
        Exception lastEx = null;

        for (String model : GEMINI_MODELS) {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + geminiApiKey;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(15))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                        .build();

                HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() == 200) {
                    JsonNode root = objectMapper.readTree(resp.body());
                    String text = root.path("candidates").get(0)
                            .path("content").path("parts").get(0)
                            .path("text").asText();
                    if (text != null && !text.isBlank()) {
                        return text.trim();
                    }
                } else {
                    lastEx = new RuntimeException("Modelo " + model + " devolvió HTTP " + resp.statusCode());
                }
            } catch (Exception ex) {
                lastEx = ex;
            }
        }
        throw lastEx != null ? lastEx : new RuntimeException("No se pudo conectar con Google Gemini API");
    }

    private String llamarMotorGenerativoCloudRag(String mensaje, String systemPrompt) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", "openai");
        payload.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", mensaje)
        ));

        String requestJson = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://text.pollinations.ai/openai"))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("User-Agent", "OdysseyCartagenaConcierge/2.0")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (resp.statusCode() != 200) {
            throw new RuntimeException("Cloud AI HTTP " + resp.statusCode());
        }

        JsonNode root = objectMapper.readTree(resp.body());
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String content = choices.get(0).path("message").path("content").asText();
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }
        throw new RuntimeException("Respuesta vacía del motor generativo");
    }

    /**
     * Motor RAG Local Inteligente con normalización sin tildes y generador de planes dinámicos.
     */
    private String sintetizarRespuestaRagLocal(String mensaje, String categoria, boolean enIngles) {
        String qNorm = normalizarTexto(mensaje);
        List<String> tokens = Arrays.stream(qNorm.split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 4 && !Set.of("para", "como", "donde", "cual", "cuales", "quiero", "puedo", "cartagena", "hacer", "recomiendas", "recomendacion").contains(w))
                .toList();

        // 1. Si el usuario pide un PLAN, ITINERARIO, CITA ROMÁNTICA o RUTA:
        if (qNorm.matches(".*(plan|itinerario|romantic|pareja|cita|atardecer|noche|ruta|recorrido|dia|dias).*")) {
            return construirPlanPersonalizadoRag(qNorm, tokens);
        }

        // 2. Buscar lugares específicos por relevancia (sin importar tildes)
        if (lugarRepository != null && !tokens.isEmpty()) {
            try {
                List<Lugar> aprobados = lugarRepository.findByEstado(Lugar.Estado.APROBADO);
                List<Lugar> coincidencias = aprobados.stream()
                        .filter(l -> puntajeRelevancia(l, tokens, qNorm) >= 4)
                        .sorted((a, b) -> Integer.compare(puntajeRelevancia(b, tokens, qNorm), puntajeRelevancia(a, tokens, qNorm)))
                        .limit(3)
                        .toList();

                if (!coincidencias.isEmpty()) {
                    StringBuilder sb = new StringBuilder("📍 **Recomendaciones verificadas en el catálogo ODYSSEY Cartagena:**\n\n");
                    for (Lugar l : coincidencias) {
                        sb.append("• **").append(l.getNombre()).append("** (").append(l.getUbicacion()).append(")\n")
                          .append("  ").append(l.getDescripcion()).append("\n\n");
                    }
                    sb.append("💡 *Tip Odyssey: Puedes explorar su ubicación exacta con audio-guía en nuestro Mapa Interactivo (`/mapa`) o suscribirte a sus novedades en su ficha.*");
                    return sb.toString();
                }
            } catch (Exception ignored) {}
        }

        // 3. Buscar en la base de conocimiento MySQL según la categoría detectada
        if (chatbotRespuestaRepository != null && !"general".equals(categoria)) {
            Optional<ChatbotRespuesta> opt = chatbotRespuestaRepository.findByClave(categoria);
            if (opt.isPresent()) {
                return opt.get().getRespuesta();
            }
        }

        // 4. Respuesta general enriquecida
        if (chatbotRespuestaRepository != null) {
            Optional<ChatbotRespuesta> saludo = chatbotRespuestaRepository.findByClave("saludo");
            if (saludo.isPresent()) {
                return saludo.get().getRespuesta();
            }
        }

        return enIngles
                ? "🌴 Welcome to Cartagena de Indias! Ask me about beaches (Islas del Rosario, Barú), historic forts (Castillo San Felipe), DATT taxi rates, Transcaribe, or Caribbean cuisine."
                : "🌴 ¡Bienvenido a Cartagena de Indias! Pregúntame sobre playas (Islas del Rosario, Barú), historia colonial (Castillo San Felipe, Murallas), transporte seguro (Transcaribe y tarifas DATT), hoteles o gastronomía caribeña.";
    }

    private String construirPlanPersonalizadoRag(String qNorm, List<String> tokens) {
        StringBuilder sb = new StringBuilder("✨ **Plan Curado por ODYSSEY Concierge — Cartagena de Indias:**\n\n");

        if (qNorm.contains("romantic") || qNorm.contains("pareja") || qNorm.contains("atardecer") || qNorm.contains("cena")) {
            sb.append("🌅 **1. Atardecer sobre las Murallas (5:00 PM - 6:30 PM):**\n")
              .append("Comienza en el **Baluarte de Santo Domingo** o en **Café del Mar**, donde la brisa caribeña y la vista panorámica del mar crean el ambiente perfecto al caer el sol.\n\n")
              .append("🎨 **2. Paseo Bohemio al Anochecer en Getsemaní (6:45 PM - 8:00 PM):**\n")
              .append("Camina desde la **Torre del Reloj** hacia el **Callejón Angosto** (bajo las sombrillas de colores) y la **Calle de la Sierpe** hasta llegar a la vibrante **Plaza de la Trinidad**, con música acústica en vivo.\n\n")
              .append("🍽️ **3. Cena de Autor en el Centro o Getsemaní (8:15 PM):**\n")
              .append("Reserva mesa en **Celele** (Calle del Espíritu Santo en Getsemaní, alta cocina caribeña contemporánea) o en **La Cevichería** (Barrio San Diego). Cierra la noche con un cóctel artesanal en un rooftop colonial.\n\n")
              .append("🚕 *Tip de movilidad: El traslado entre Centro Histórico y Getsemaní se hace caminando de forma segura (5 min) o en taxi oficial DATT (~$10.000 COP).*");
            return sb.toString();
        }

        sb.append("🏛️ **Mañana Colonial (8:30 AM - 12:30 PM):**\n")
          .append("Inicia temprano en el **Castillo de San Felipe de Barajas** para evitar el sol fuerte, y luego cruza la **Torre del Reloj** hacia la **Plaza de los Coches** y el **Palacio de la Inquisición**.\n\n")
          .append("🍤 **Almuerzo Caribeño (1:00 PM - 3:00 PM):**\n")
          .append("Prueba un ceviche fresco en **La Cevichería** (San Diego) o arroz con coco y posta cartagenera, seguido de cocadas tradicionales en el **Portal de los Dulces**.\n\n")
          .append("🌇 **Tarde y Noche Cultural (4:30 PM - 9:00 PM):**\n")
          .append("Sube a las murallas en el **Baluarte de Santo Domingo** para el atardecer y termina la noche explorando los murales, artistas callejeros y gastronomía de **Getsemaní** (Plaza de la Trinidad y **Celele**).");
        return sb.toString();
    }

    private void registrarLogChatbot(String mensaje, String respuesta, long duracionMs,
                                     String categoria, String ip, String idioma, boolean exito, String error) {
        if (chatbotLogRepository == null) return;
        try {
            ChatbotLog chatLog = new ChatbotLog();
            chatLog.setMensajeUsuario(mensaje);
            chatLog.setRespuestaIA(respuesta);
            chatLog.setTiempoRespuestaMs(duracionMs);
            chatLog.setCategoria(categoria);
            chatLog.setIpRemota(ip);
            chatLog.setIdioma(idioma);
            chatLog.setExito(exito);
            chatLog.setErrorDetalle(error);
            chatbotLogRepository.save(chatLog);
        } catch (Exception ex) {
            log.warn("No se pudo guardar el log de Chatbot en MongoDB: {}", ex.getMessage());
        }
    }

    public String detectarCategoria(String msgNorm) {
        if (msgNorm.matches(".*(hola|buenos dias|buenas|hi|hey|saludos|hello|good morning).*")) return "saludo";
        if (msgNorm.matches(".*(clima|lluvi|temperatur|cuando visit|mejor epoca|weather|rain|when to visit).*")) return "clima";
        if (msgNorm.matches(".*(playa|baru|rosario|bocagrande|cholon|boquilla|beach|island).*")) return "playas";
        if (msgNorm.matches(".*(transport|mover|llegar|bus|transcaribe|taxi|lancha|muelle|bodeguita|aeropuerto|get around|boat|airport).*")) return "transporte";
        if (msgNorm.matches(".*(hotel|aloj|hostal|donde dormir|santa clara|charleston|bastion|stay|sleep|accommodation).*")) return "hoteles";
        if (msgNorm.matches(".*(histor|coloni|murall|castill|san felipe|bovedas|inquisicion|popa|reloj|wall|castle|fort).*")) return "historico";
        if (msgNorm.matches(".*(actividad|hacer|tour|buceo|snorkel|kayak|manglar|chiva|catamaran|activity|diving|things to do).*")) return "actividades";
        if (msgNorm.matches(".*(evento|festival|hay festival|ficci|independencia|noviembre|frito|event).*")) return "eventos";
        if (msgNorm.matches(".*(costo|precio|caro|barato|dinero|presupuesto|cuanto cuesta|cost|price|budget|cheap).*")) return "costo";
        if (msgNorm.matches(".*(comer|comida|gastronom|restaur|ceviche|arepa|posta|celele|dulce|bazurto|food|eat|restaurant).*")) return "gastronomia";
        if (msgNorm.matches(".*(segur|estafa|peligr|policia|consejo|cuidado|safe|scam).*")) return "seguridad";
        if (msgNorm.matches(".*(barrio|getsemani|san diego|manga|cabrero|neighborhood).*")) return "barrios";
        if (msgNorm.matches(".*(adios|gracias|bye|chao|goodbye|thank).*")) return "despedida";
        return "general";
    }
}
