package com.odyxs.vg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odyxs.vg.domain.mongo.ChatbotLog;
import com.odyxs.vg.entity.ChatbotRespuesta;
import com.odyxs.vg.repository.ChatbotRespuestaRepository;
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
import java.time.Duration;
import java.util.*;

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
    private RedisMetricsService redisMetricsService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final String ODYSSEY_SYSTEM_ES =
            "Eres Odyssey, asistente turístico experto de Cartagena de Indias, Colombia. " +
            "Responde SIEMPRE en español, de forma cálida, amigable y concisa (máx. 3 párrafos). " +
            "Eres experto en: playas (Bocagrande, Barú, Islas del Rosario, Playa Blanca), " +
            "historia colonial (Castillo de San Felipe, murallas, Getsemaní), " +
            "transporte (taxis, Transcaribe, lanchas, aeropuerto Rafael Núñez), " +
            "hoteles y hospedaje, gastronomía caribeña (ceviche, patacón, arepas de huevo, cazuela de mariscos), " +
            "festivales (Hay Festival enero, FICCI marzo, Fiestas Independencia 11 de noviembre), " +
            "actividades (buceo, snorkel, chiva rumbera, kayak, tours en velero). " +
            "Usa emojis con moderación.";

    private static final String ODYSSEY_SYSTEM_EN =
            "You are Odyssey, the intelligent tourist assistant for Cartagena de Indias, Colombia. " +
            "ALWAYS respond in English, in a friendly and concise way (max. 3 paragraphs). " +
            "You are an expert in: beaches (Bocagrande, Barú, Rosario Islands, Playa Blanca), " +
            "colonial history (Castillo de San Felipe, city walls, Getsemaní neighborhood), " +
            "transport (taxis, Transcaribe bus, boats, Rafael Núñez Airport), " +
            "hotels and accommodation, Caribbean gastronomy (ceviche, patacón, egg arepas, seafood casserole), " +
            "festivals (Hay Festival in January, FICCI in March, Independence Day November 11), " +
            "activities (diving, snorkeling, chiva rumbera, kayak, sailboat tours). " +
            "Use emojis in moderation.";

    /**
     * Procesa la consulta del turista con Gemini IA, telemetría NoSQL en MongoDB y caché/rate-limiting en Redis.
     */
    public String responder(String mensaje, boolean enIngles, String clientIp) {
        if (mensaje == null || mensaje.isBlank()) {
            return enIngles ? "How can I help you explore Cartagena? 🌊" : "¿En qué puedo ayudarte a explorar Cartagena? 🌊";
        }

        // 1. Verificación de Rate Limiting con Redis
        if (redisMetricsService != null && !redisMetricsService.verificarLimiteRateChatbot(clientIp)) {
            return enIngles
                    ? "⚠️ Too many requests. Please wait a minute before asking again."
                    : "⚠️ Has enviado muchas preguntas en poco tiempo. Por favor espera un minuto.";
        }

        String categoriaDetectada = detectarCategoria(mensaje.toLowerCase());
        String systemPrompt = enIngles ? ODYSSEY_SYSTEM_EN : ODYSSEY_SYSTEM_ES;

        long inicio = System.currentTimeMillis();
        String respuesta = null;
        boolean exito = false;
        String errorMsg = null;

        // 2. Intento de respuesta con Google Gemini
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            try {
                respuesta = llamarGeminiAPI(mensaje, systemPrompt);
                exito = true;
            } catch (Exception ex) {
                log.warn("Fallo al llamar Gemini API: {}. Recurriendo a base de conocimiento local.", ex.getMessage());
                errorMsg = ex.getMessage();
            }
        }

        // 3. Fallback inteligente a base de datos MySQL local si Gemini no está configurado o falla
        if (!exito || respuesta == null || respuesta.isBlank()) {
            respuesta = obtenerRespuestaFallback(categoriaDetectada, enIngles);
        }

        long duracionMs = System.currentTimeMillis() - inicio;

        // 4. Registro de telemetría NoSQL en MongoDB
        registrarLogChatbot(mensaje, respuesta, duracionMs, categoriaDetectada, clientIp, enIngles ? "en" : "es", exito, errorMsg);

        return respuesta;
    }

    private String llamarGeminiAPI(String mensaje, String systemPrompt) throws Exception {
        Map<String, Object> systemPart = Map.of("text", systemPrompt);
        Map<String, Object> systemInstruction = Map.of("parts", List.of(systemPart));

        Map<String, Object> userPart = Map.of("text", mensaje);
        Map<String, Object> userContent = Map.of("role", "user", "parts", List.of(userPart));

        Map<String, Object> generationConfig = Map.of(
                "maxOutputTokens", 600,
                "temperature", 0.7
        );

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("systemInstruction", systemInstruction);
        payload.put("contents", List.of(userContent));
        payload.put("generationConfig", generationConfig);

        String requestJson = objectMapper.writeValueAsString(payload);
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Gemini API HTTP " + resp.statusCode() + ": " + resp.body());
        }

        JsonNode root = objectMapper.readTree(resp.body());
        return root.path("candidates").get(0)
                .path("content").path("parts").get(0)
                .path("text").asText();
    }

    private String obtenerRespuestaFallback(String categoria, boolean enIngles) {
        if (chatbotRespuestaRepository != null) {
            Optional<ChatbotRespuesta> opt = chatbotRespuestaRepository.findByClave(categoria);
            if (opt.isPresent()) {
                return opt.get().getRespuesta();
            }
        }
        return enIngles
                ? "🌴 Welcome to Cartagena! Try asking me about: beaches, historic sites, transport, hotels, activities, or Caribbean cuisine."
                : "🌴 ¡Bienvenido a Cartagena de Indias! Prueba preguntarme sobre: playas, historia, transporte, hoteles, actividades o gastronomía caribeña.";
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

    public String detectarCategoria(String msg) {
        if (msg.matches(".*(hola|buenos d[ií]as|buenas|hi|hey|saludos|hello|good morning).*")) return "saludo";
        if (msg.matches(".*(clima|lluvi|temperatur|cu[aá]ndo visit|weather|rain|when to visit).*")) return "clima";
        if (msg.matches(".*(playa|bar[uú]|rosario|bocagrande|beach|island).*")) return "playas";
        if (msg.matches(".*(transport|mover|llegar|bus|taxi|yate|get around|boat|airport).*")) return "transporte";
        if (msg.matches(".*(hotel|aloj|hostal|donde dormir|stay|sleep|accommodation).*")) return "hoteles";
        if (msg.matches(".*(histor|coloni|murall|castill|fundaci|wall|castle|fort).*")) return "historico";
        if (msg.matches(".*(actividad|hacer|tour|buceo|kayak|activity|diving|things to do).*")) return "actividades";
        if (msg.matches(".*(evento|festival|hay festival|ficci|event).*")) return "eventos";
        if (msg.matches(".*(costo|precio|caro|barato|dinero|cost|price|budget|cheap).*")) return "costo";
        if (msg.matches(".*(comer|comida|gastronom|restaur|ceviche|food|eat|restaurant).*")) return "gastronomia";
        if (msg.matches(".*(adi[oó]s|gracias|bye|chao|goodbye|thank).*")) return "despedida";
        return "general";
    }
}
