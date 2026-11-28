package com.odyxs.vg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Motor Multimodal de Visión IA & Escudo Anti-Sobrecobros Turísticos (Odyssey v5.0).
 * Procesa imágenes en memoria RAM (sin guardar fotos privadas en disco, cumpliendo Privacy by Design)
 * mediante Google Gemini Multimodal Vision API y cuenta con un auditor determinístico local basado en
 * precios oficiales de referencia en Cartagena (Ley 1480 de 2011 Art. 26 - Información Pública de Precios).
 */
@Service
public class GeminiVisionService {

    private static final Logger log = LoggerFactory.getLogger(GeminiVisionService.class);
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final double TRM_REFERENCIA_COP_USD = 4150.0;
    private static final Set<String> ALLOWED_MIMES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Pattern PRECIO_PATTERN = Pattern.compile("(\\d{1,3}(?:[.,]\\d{3})+|\\d{4,7})");

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public record TarifaReferenciaDto(
            String item,
            String categoria,
            long minimoJustoCop,
            long maximoJustoCop,
            long umbralAlertaCop,
            String consejoLegal
    ) {}

    public record ResultadoVisionDto(
            String modo,
            String nivelAlerta,
            String veredictoTitulo,
            String analisisDetallado,
            long montoDetectadoCop,
            double equivalenteUsd,
            List<String> recomendacionesAccionables,
            String motorUtilizado
    ) {}

    private static final List<TarifaReferenciaDto> CATALOGO_PRECIOS_REFERENCIA = List.of(
            new TarifaReferenciaDto("Pescado frito típico (Mojarra / Pargo con arroz con coco y patacón)", "Gastronomía en Playa",
                    45_000, 88_000, 115_000, "Exige siempre carta física con precios impresos antes de ordenar (Ley 1480 Art. 26)."),
            new TarifaReferenciaDto("Ceviche caribeño / Coctel de camarón mediano", "Gastronomía",
                    30_000, 65_000, 90_000, "Verifica si el servicio voluntario (propina máx. 10%) fue incluido previa consulta."),
            new TarifaReferenciaDto("Cerveza nacional unidad (playa o terraza estándar)", "Bebidas",
                    7_000, 18_000, 30_000, "En playas públicas nadie puede obligarte a consumo mínimo por transitar la arena."),
            new TarifaReferenciaDto("Limonada de coco / Jugo natural", "Bebidas",
                    12_000, 24_000, 35_000, "Confirma el precio unitario en pesos colombianos (COP), nunca en cifras ambiguas."),
            new TarifaReferenciaDto("Alquiler de carpa / parasol con 2 sillas (día completo)", "Playas",
                    50_000, 90_000, 130_000, "Acuerda el valor total por escrito o en recibo antes de ocupar la carpa."),
            new TarifaReferenciaDto("Masaje relajante en playa (sesión acordada)", "Servicios en Playa",
                    40_000, 75_000, 100_000, "No aceptes 'muestras gratis de 1 minuto'; suelen derivar en cobros no pactados."),
            new TarifaReferenciaDto("Taxi oficial DATT Aeropuerto → Centro Histórico / Getsemaní", "Transporte Oficial",
                    15_500, 22_000, 32_000, "Solicita el ticket impreso de tarifa oficial en la casilla de despacho del aeropuerto."),
            new TarifaReferenciaDto("Taxi oficial DATT Aeropuerto → Bocagrande / Laguito", "Transporte Oficial",
                    23_000, 30_000, 42_000, "Los taxis en Cartagena no usan taxímetro: se rigen por tabla zonal DATT."),
            new TarifaReferenciaDto("Lancha autorizada Muelle Bodeguita ↔ Islas del Rosario (ida y regreso)", "Transporte Marítimo",
                    90_000, 145_000, 195_000, "Embarca únicamente por el Muelle de la Bodeguita pagando el impuesto portuario oficial.")
    );

    public List<TarifaReferenciaDto> obtenerTarifasReferencia() {
        return CATALOGO_PRECIOS_REFERENCIA;
    }

    public ResultadoVisionDto analizarEntradaMultimodal(MultipartFile imagen, String modoRaw, String descripcionTexto) {
        String modo = "PATRIMONIO".equalsIgnoreCase(modoRaw) ? "PATRIMONIO" : "PRECIOS";
        String texto = descripcionTexto != null ? descripcionTexto.trim() : "";

        byte[] imageBytes = null;
        String mimeType = null;

        if (imagen != null && !imagen.isEmpty()) {
            if (imagen.getSize() > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("La imagen excede el límite seguro de 5 MB.");
            }
            String ct = imagen.getContentType() != null ? imagen.getContentType().toLowerCase(Locale.ROOT) : "";
            if (!ALLOWED_MIMES.contains(ct)) {
                throw new IllegalArgumentException("Formato de imagen no permitido. Usa JPG, PNG o WEBP.");
            }
            try {
                imageBytes = imagen.getBytes();
                mimeType = "image/jpg".equals(ct) ? "image/jpeg" : ct;
            } catch (Exception e) {
                throw new IllegalArgumentException("No fue posible leer la imagen enviada.");
            }
        }

        if ((imageBytes == null || imageBytes.length == 0) && texto.isBlank()) {
            throw new IllegalArgumentException("Adjunta una fotografía o escribe el detalle del plato, monumento o cuenta a verificar.");
        }

        if (tieneGeminiKeyValida() && imageBytes != null && imageBytes.length > 0) {
            ResultadoVisionDto viaGemini = invocarGeminiMultimodal(imageBytes, mimeType, modo, texto);
            if (viaGemini != null) {
                registrarMetrica(modo, "gemini_vision");
                return viaGemini;
            }
        }

        registrarMetrica(modo, "local_deterministic");
        return "PATRIMONIO".equals(modo)
                ? analizarPatrimonioLocal(texto, imageBytes != null ? imagen.getOriginalFilename() : "")
                : auditarPreciosLocal(texto);
    }

    private ResultadoVisionDto invocarGeminiMultimodal(byte[] imageBytes, String mimeType, String modo, String contextoUsuario) {
        try {
            String base64Img = Base64.getEncoder().encodeToString(imageBytes);
            String promptSistema = "PATRIMONIO".equals(modo)
                    ? "Eres el historiador y guía patrimonial oficial de ODYSSEY en Cartagena de Indias (UNESCO 1984). "
                      + "Identifica el monumento, baluarte, calle colonial o plato caribeño de la foto. Explica en español claro: "
                      + "1) Qué es e historia clave, 2) Mejor hora para visitarlo o probarlo, 3) Consejo local auténtico. Contexto adicional: " + contextoUsuario
                    : "Eres el Auditor de Protección al Turista de ODYSSEY en Cartagena de Indias conforme a la Ley 1480 de 2011 (Estatuto del Consumidor). "
                      + "Lee los precios en pesos colombianos (COP) del menú o recibo en la imagen, suma el total aproximado, compáralo con rangos reales de Cartagena "
                      + "(pescado frito 45k-88k COP, cerveza 7k-18k COP, limonada coco 12k-24k COP, taxi urbano 10k-28k COP) e indica claramente si es PRECIO JUSTO o ALERTA DE SOBRECOBRO. "
                      + "Contexto adicional del viajero: " + contextoUsuario;

            Map<String, Object> inlineData = Map.of("mime_type", mimeType, "data", base64Img);
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(
                                    Map.of("text", promptSistema),
                                    Map.of("inline_data", inlineData)
                            )
                    )),
                    "generationConfig", Map.of("temperature", 0.25, "maxOutputTokens", 650)
            );

            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                String textoIa = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
                if (!textoIa.isBlank()) {
                    boolean alerta = textoIa.toUpperCase(Locale.ROOT).contains("SOBRECOBRO")
                            || textoIa.toUpperCase(Locale.ROOT).contains("EXCESIVO");
                    long monto = extraerMayorMontoCop(contextoUsuario + " " + textoIa);
                    return new ResultadoVisionDto(
                            modo,
                            "PATRIMONIO".equals(modo) ? "PATRIMONIO_IDENTIFICADO" : (alerta ? "ALERTA_SOBRECOBRO" : "PRECIO_JUSTO"),
                            "PATRIMONIO".equals(modo) ? "🏛️ Reconocimiento Patrimonial Multimodal" : (alerta ? "🚨 Posible Sobrecobro Detectado" : "✅ Análisis de Cuenta Verificado"),
                            textoIa.trim(),
                            monto,
                            Math.round((monto / TRM_REFERENCIA_COP_USD) * 100.0) / 100.0,
                            construirConsejosDefensaTurista(alerta),
                            "Google Gemini 2.0 Flash Vision (Multimodal)"
                    );
                }
            }
        } catch (Exception ex) {
            log.debug("Fallback local en GeminiVisionService: {}", ex.getMessage());
        }
        return null;
    }

    private ResultadoVisionDto auditarPreciosLocal(String textoRaw) {
        String norm = GeminiAiService.normalizarTexto(textoRaw);
        long mayorMonto = extraerMayorMontoCop(textoRaw);
        long sumaMontos = sumarMontosCop(textoRaw);
        long referenciaEvaluada = Math.max(mayorMonto, sumaMontos);

        String nivel = "PRECIO_JUSTO";
        String titulo = "✅ Rango Compatible con Tarifas de Referencia";
        StringBuilder detalle = new StringBuilder();

        boolean detectoItemEspecifico = false;
        for (TarifaReferenciaDto ref : CATALOGO_PRECIOS_REFERENCIA) {
            String itemNorm = GeminiAiService.normalizarTexto(ref.item());
            String palabraClave = itemNorm.split(" ")[0];
            if (norm.contains(palabraClave)
                    || (norm.contains("mojarra") && itemNorm.contains("mojarra"))
                    || (norm.contains("pargo") && itemNorm.contains("pargo"))
                    || (norm.contains("cerveza") && itemNorm.contains("cerveza"))
                    || (norm.contains("taxi") && itemNorm.contains("taxi"))
                    || (norm.contains("carpa") && itemNorm.contains("carpa"))
                    || (norm.contains("masaje") && itemNorm.contains("masaje"))
                    || (norm.contains("lancha") && itemNorm.contains("lancha"))
                    || (norm.contains("ceviche") && itemNorm.contains("ceviche"))) {
                detectoItemEspecifico = true;
                detalle.append("• Referencia oficial para «").append(ref.item()).append("»: entre $")
                        .append(String.format(Locale.US, "%,d", ref.minimoJustoCop()).replace(',', '.'))
                        .append(" y $")
                        .append(String.format(Locale.US, "%,d", ref.maximoJustoCop()).replace(',', '.'))
                        .append(" COP. ");

                if (mayorMonto >= ref.umbralAlertaCop()) {
                    nivel = "ALERTA_SOBRECOBRO";
                    titulo = "🚨 Alerta Roja: Valor por encima del umbral justo en Cartagena";
                    detalle.append("El cobro detectado ($")
                            .append(String.format(Locale.US, "%,d", mayorMonto).replace(',', '.'))
                            .append(" COP) supera ampliamente el tope de alerta ($")
                            .append(String.format(Locale.US, "%,d", ref.umbralAlertaCop()).replace(',', '.'))
                            .append(" COP). ");
                } else if (mayorMonto > ref.maximoJustoCop()) {
                    if (!"ALERTA_SOBRECOBRO".equals(nivel)) {
                        nivel = "REVISAR_DETALLE";
                        titulo = "⚠️ Precio en franja alta (Restaurante de autor / Hotel 5★ o recargo)";
                    }
                }
                detalle.append(ref.consejoLegal()).append("\n");
            }
        }

        if (!detectoItemEspecifico) {
            if (referenciaEvaluada >= 250_000) {
                nivel = "ALERTA_SOBRECOBRO";
                titulo = "🚨 Cuenta elevada detectada: verifica cada ítem antes de pagar";
                detalle.append("Se detectó un monto total/unitario de $")
                        .append(String.format(Locale.US, "%,d", referenciaEvaluada).replace(',', '.'))
                        .append(" COP. Verifica que la propina voluntaria no supere el 10% y que cada plato coincida con la carta impresa.");
            } else if (referenciaEvaluada > 0) {
                detalle.append("Monto analizado: $")
                        .append(String.format(Locale.US, "%,d", referenciaEvaluada).replace(',', '.'))
                        .append(" COP. Se encuentra dentro de parámetros habituales para consumo individual o traslado urbano en Cartagena.");
            } else {
                nivel = "REVISAR_DETALLE";
                titulo = "📋 Consulta de Precios Justos Activada";
                detalle.append("Imagen recibida en memoria segura. Para un desglose numérico exacto sin conexión externa, incluye también el valor cobrado (ej. 'mojarra 150000, cerveza 25000').");
            }
        }

        double usd = Math.round((referenciaEvaluada / TRM_REFERENCIA_COP_USD) * 100.0) / 100.0;
        return new ResultadoVisionDto(
                "PRECIOS",
                nivel,
                titulo,
                detalle.toString().trim(),
                referenciaEvaluada,
                usd,
                construirConsejosDefensaTurista("ALERTA_SOBRECOBRO".equals(nivel)),
                "Motor Auditor Anti-Sobrecobros Odyssey v5.0 (Ley 1480 Art. 26)"
        );
    }

    private ResultadoVisionDto analizarPatrimonioLocal(String textoRaw, String nombreArchivo) {
        String fuente = GeminiAiService.normalizarTexto(textoRaw + " " + (nombreArchivo != null ? nombreArchivo : ""));
        String titulo = "🏛️ Guía Patrimonial UNESCO — Cartagena de Indias";
        String analisis;

        if (fuente.contains("castillo") || fuente.contains("felipe") || fuente.contains("canon") || fuente.contains("tunel")) {
            titulo = "🏰 Castillo de San Felipe de Barajas (Siglo XVI–XVII)";
            analisis = "La obra cumbre de la ingeniería militar española en América, construida sobre el cerro de San Lázaro. Sus galerías subterráneas fueron diseñadas acústicamente para escuchar pasos enemigos. Mejor horario: 8:00 AM o 4:15 PM.";
        } else if (fuente.contains("reloj") || fuente.contains("portal") || fuente.contains("coches")) {
            titulo = "🕰️ Torre del Reloj y Portal de los Dulces";
            analisis = "Entrada principal de la Ciudad Amurallada que conectaba el antiguo puente levadizo con Getsemaní. Ideal para iniciar recorridos peatonales gratuitos al atardecer.";
        } else if (fuente.contains("muralla") || fuente.contains("baluarte") || fuente.contains("domingo")) {
            titulo = "🌅 Baluarte de Santo Domingo y Cordón Amurallado";
            analisis = " más de 11 km de fortificaciones coralinas declaradas Patrimonio de la Humanidad por la UNESCO en 1984. Acceso peatonal público y gratuito.";
        } else {
            analisis = "Fotografía procesada en memoria volátil segura. Estás explorando el conjunto monumental de Cartagena de Indias (UNESCO 1984). Abre `/mapa` para trazar la ruta peatonal exacta hacia los 26 hitos históricos cercanos.";
        }

        return new ResultadoVisionDto(
                "PATRIMONIO",
                "PATRIMONIO_IDENTIFICADO",
                titulo,
                analisis,
                0L,
                0.0,
                List.of(
                        "Explora la distancia GPS exacta y audio-guía en /mapa.",
                        "Recuerda llevar hidratación y bloqueador solar entre las 10:00 AM y las 3:30 PM."
                ),
                "Reconocedor Patrimonial Odyssey v5.0 (Catálogo UNESCO)"
        );
    }

    private static List<String> construirConsejosDefensaTurista(boolean alertaRoja) {
        if (alertaRoja) {
            return List.of(
                    "No realices el pago sin antes fotografiar la carta física publicada (obligatoria según Art. 26 Ley 1480 de 2011).",
                    "Comunícate de inmediato con la Policía de Turismo de Cartagena: +57 (605) 655 0211 o Línea 123.",
                    "Recuerda que la propina ('servicio') en Colombia es 100% voluntaria y máximo sugerida del 10%."
            );
        }
        return List.of(
                "Exige siempre factura o recibo discriminado en pesos colombianos (COP).",
                "Verifica en paginas o datáfonos que el monto digitado coincida exactamente con los ceros en COP."
        );
    }

    private static long extraerMayorMontoCop(String texto) {
        if (texto == null || texto.isBlank()) return 0L;
        Matcher m = PRECIO_PATTERN.matcher(texto);
        long max = 0L;
        while (m.find()) {
            String limpio = m.group(1).replace(".", "").replace(",", "");
            try {
                long val = Long.parseLong(limpio);
                if (val >= 2000 && val <= 50_000_000L && val > max) {
                    max = val;
                }
            } catch (NumberFormatException ignored) {}
        }
        return max;
    }

    private static long sumarMontosCop(String texto) {
        if (texto == null || texto.isBlank()) return 0L;
        Matcher m = PRECIO_PATTERN.matcher(texto);
        long suma = 0L;
        while (m.find()) {
            String limpio = m.group(1).replace(".", "").replace(",", "");
            try {
                long val = Long.parseLong(limpio);
                if (val >= 2000 && val <= 20_000_000L) {
                    suma += val;
                }
            } catch (NumberFormatException ignored) {}
        }
        return suma;
    }

    private boolean tieneGeminiKeyValida() {
        return geminiApiKey != null && !geminiApiKey.isBlank()
                && !geminiApiKey.contains("TU_KEY") && !geminiApiKey.contains("tu_api_key");
    }

    private void registrarMetrica(String modo, String motor) {
        if (meterRegistry != null) {
            meterRegistry.counter("odyssey.vision.scans", "modo", modo, "motor", motor).increment();
        }
    }
}
