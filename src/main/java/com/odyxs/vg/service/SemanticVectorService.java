package com.odyxs.vg.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odyxs.vg.entity.Lugar;
import com.odyxs.vg.repository.LugarRepository;
import io.micrometer.core.instrument.MeterRegistry;
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
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SemanticVectorService {

    private static final Logger log = LoggerFactory.getLogger(SemanticVectorService.class);
    private static final int LOCAL_VECTOR_DIM = 256;

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Autowired(required = false)
    private LugarRepository lugarRepository;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final Map<String, float[]> cacheEmbeddingsGemini = new ConcurrentHashMap<>();

    public record ResultadoSemanticoDto(
            Long lugarId,
            String nombre,
            String categoria,
            String ubicacion,
            String descripcion,
            String imagenUrl,
            double similitudCoseno,
            int porcentajeAfinidad,
            String motorVectorial
    ) {}

    private record DocumentoCatalogo(
            Long lugarId,
            String nombre,
            String categoria,
            String ubicacion,
            String descripcion,
            String imagenUrl,
            String corpusEnriquecido
    ) {}

    private static final Map<String, String> CONCEPTOS_SEMANTICOS = Map.ofEntries(
            Map.entry("matrimonio", "romantico pareja atardecer cena lujo velas mirador baluarte"),
            Map.entry("aniversario", "romantico pareja cena autor lujo atardecer hotel boutique velero"),
            Map.entry("cita", "romantico pareja cafe atardecer murallas coctel getsemani"),
            Map.entry("romantico", "pareja atardecer murallas baluarte santo domingo cafe del mar celele"),
            Map.entry("pareja", "romantico atardecer cena mirador paseo colonial santa clara"),
            Map.entry("barato", "economico gratis libre plaza caminar callejero fritos transcaribe"),
            Map.entry("economico", "barato gratis plaza trinidad reloj murallas caminar portal dulces"),
            Map.entry("gratis", "acceso libre murallas torre reloj plaza trinidad getsemani caminar"),
            Map.entry("ninos", "familia seguro tranquilo museo oro naval playa bocagrande acuario"),
            Map.entry("familia", "ninos tranquilo cultural museo castillo paseo seguro"),
            Map.entry("piratas", "historia militar fortaleza canones tuneles castillo san felipe bocachica"),
            Map.entry("militar", "fortaleza murallas castillo san felipe fuerte san fernando canones"),
            Map.entry("snorkel", "arrecife coral aguas cristalinas turquesa islas rosario baru buceo"),
            Map.entry("buceo", "snorkel mar arrecife coral islas rosario naturaleza acuatico"),
            Map.entry("fiesta", "rumba musica baile champeta noche chiva cholon getsemani cocteles"),
            Map.entry("tranquilo", "paz silencio relax descanso brisa naturaleza convento popa boquilla"),
            Map.entry("comida", "gastronomia restaurante ceviche mariscos dulce tipico bazurto celele"),
            Map.entry("hambre", "comer restaurante gastronomia ceviche arepa almuerzo cena"),
            Map.entry("foto", "panoramica mirador instagram postal colores balcones atardecer popa")
    );

    private static final List<DocumentoCatalogo> CATALOGO_BASE_SEMANTICO = List.of(
            new DocumentoCatalogo(1L, "Castillo de San Felipe de Barajas", "Sitios Históricos", "Pie de la Popa",
                    "La mayor fortaleza militar colonial de América con red de túneles subterráneos, cañones e historia de defensa contra corsarios y piratas.",
                    "/img/hero-castillo.jpg", "historia militar fortaleza tuneles canones piratas mirador familia cultural"),
            new DocumentoCatalogo(2L, "Torre del Reloj y Ciudad Amurallada", "Sitios Históricos", "Centro Histórico",
                    "Puerta principal a la ciudad colonial UNESCO, plazas públicas gratuitas, balcones coloniales y paseos peatonales.",
                    "/img/about-torre-reloj.jpg", "gratis economico caminar historia colonial fotos balcones centro"),
            new DocumentoCatalogo(3L, "Baluarte de Santo Domingo y Murallas", "Sitios Históricos", "Centro Histórico",
                    "Murallas frente al mar Caribe ideales para contemplar el atardecer en pareja de forma libre y romántica.",
                    "/img/hero-murallas.jpg", "romantico pareja atardecer matrimonio cita gratis brisa mar fotos"),
            new DocumentoCatalogo(12L, "Plaza de la Trinidad y Barrio Getsemaní", "Cultura y Arte", "Getsemaní",
                    "Epicentro bohemio con murales de arte urbano, música en vivo, ambiente nocturno, comida callejera económica y cultura local.",
                    "/img/act-getsemani.jpg", "economico barato fiesta musica arte murales joven bohemio noche"),
            new DocumentoCatalogo(13L, "Playa Blanca (Península de Barú)", "Playas", "Isla Barú",
                    "Playa caribeña de arena blanca y mar turquesa cálido para pasadía de sol y descanso frente al mar.",
                    "/img/hero-playa-blanca.jpg", "playa mar arena sol nadar descanso caribe turquesa"),
            new DocumentoCatalogo(14L, "Parque Nacional Natural Islas del Rosario", "Naturaleza y Aventura", "Archipiélago del Rosario",
                    "28 islas de arrecifes coralinos protegidos, aguas cristalinas para snorkel, buceo, kayak y escapadas románticas.",
                    "/img/act-islas-rosario.jpg", "snorkel buceo arrecife coral isla aventura naturaleza pareja"),
            new DocumentoCatalogo(16L, "Manglares de La Boquilla", "Naturaleza y Aventura", "Zona Norte",
                    "Pueblo pesquero afrocartagenero con paseos tranquilos en canoa por túneles de manglar, avistamiento de aves y kitesurf.",
                    "/img/act-kayak-manglares.jpg", "tranquilo naturaleza ecoturismo kayak aves pesca familia"),
            new DocumentoCatalogo(17L, "Isla de Cholón", "Playas", "Barú",
                    "Ensenada náutica famosa por fiestas en yates, música caribeña sobre el agua y ambiente rumbero.",
                    "/img/act-islas-rosario.jpg", "fiesta rumba baile yates lancha musica amigos"),
            new DocumentoCatalogo(20L, "Portal de los Dulces", "Gastronomía", "Plaza de los Coches",
                    "Pasaje colonial tradicional lleno de cocadas, dulces típicos costeños económicos e historia popular.",
                    "/img/gastro-arepa.jpg", "comida dulce tipico economico barato familia ninos"),
            new DocumentoCatalogo(21L, "La Cevichería", "Gastronomía", "Barrio San Diego",
                    "Restaurante icónico de mariscos frescos, ceviches caribeños y ambiente colonial acogedor.",
                    "/img/gastro-ceviche.jpg", "comida hambre gastronomia mariscos ceviche almuerzo cena"),
            new DocumentoCatalogo(22L, "Restaurante Celele", "Gastronomía", "Getsemaní",
                    "Alta cocina contemporánea de investigación del Caribe colombiano, ideal para cenas especiales, aniversarios y gastronomía de autor.",
                    "/img/gastro-arroz-mariscos.jpg", "comida cena autor lujo aniversario matrimonio pareja romantico gourmet"),
            new DocumentoCatalogo(23L, "Café del Mar (Baluarte)", "Gastronomía", "Murallas de San Diego",
                    "Terraza lounge sobre la muralla con vista panorámica al océano para pedir matrimonio, brindar al atardecer y escuchar chillout.",
                    "/img/hero-murallas.jpg", "romantico pareja matrimonio aniversario cita atardecer cocteles mar"),
            new DocumentoCatalogo(24L, "Sofitel Legend Santa Clara", "Hoteles", "Barrio San Diego",
                    "Antiguo convento del siglo XVII transformado en hotel de ultralujo con claustro colonial, spa y alta hospitalidad.",
                    "/img/hotel-sofitel.jpg", "hotel lujo aniversario boda pareja tranquilo historia spa")
    );

    public List<ResultadoSemanticoDto> buscarSemanticamente(String consulta, int topK) {
        if (consulta == null || consulta.isBlank()) return List.of();
        int limite = Math.max(1, Math.min(topK, 12));

        List<DocumentoCatalogo> documentos = construirDocumentosActivos();
        boolean usarGemini = tieneGeminiKeyValida();
        String motor = "Odyssey Dense Subword+Concept Embeddings (256d Cosine)";

        float[] vectorConsulta = null;
        if (usarGemini) {
            vectorConsulta = obtenerEmbeddingGemini("query: " + consulta);
            if (vectorConsulta != null) {
                motor = "Google Gemini text-embedding-004 (768d Cosine)";
            }
        }
        if (vectorConsulta == null) {
            vectorConsulta = generarEmbeddingLocal256d(consulta);
            usarGemini = false;
        }

        final float[] qVec = vectorConsulta;
        final boolean modoGemini = usarGemini;
        final String motorFinal = motor;

        if (meterRegistry != null) {
            meterRegistry.counter("odyssey.ai.vector_search", "motor", modoGemini ? "gemini_768d" : "local_256d").increment();
        }

        return documentos.stream()
                .map(doc -> {
                    String textoDoc = doc.nombre() + ". " + doc.categoria() + ". " + doc.ubicacion()
                            + ". " + doc.descripcion() + " " + doc.corpusEnriquecido();
                    float[] dVec = modoGemini
                            ? obtenerEmbeddingGemini("doc:" + doc.lugarId() + ":" + textoDoc)
                            : null;
                    float[] qEfectivo = qVec;
                    if (dVec == null) {
                        dVec = generarEmbeddingLocal256d(textoDoc);
                        if (qEfectivo.length != LOCAL_VECTOR_DIM) {
                            qEfectivo = generarEmbeddingLocal256d(consulta);
                        }
                    }
                    double sim = Math.max(0.0, similitudCoseno(qEfectivo, dVec));
                    int afinidad = (int) Math.min(99, Math.round(sim * 100.0));
                    return new ResultadoSemanticoDto(
                            doc.lugarId(),
                            doc.nombre(),
                            doc.categoria(),
                            doc.ubicacion(),
                            doc.descripcion(),
                            doc.imagenUrl(),
                            Math.round(sim * 10000.0) / 10000.0,
                            afinidad,
                            motorFinal
                    );
                })
                .sorted(Comparator.comparingDouble(ResultadoSemanticoDto::similitudCoseno).reversed())
                .limit(limite)
                .toList();
    }

    public float[] generarEmbeddingLocal256d(String textoRaw) {
        float[] vec = new float[LOCAL_VECTOR_DIM];
        if (textoRaw == null || textoRaw.isBlank()) return vec;

        String limpio = GeminiAiService.normalizarTexto(textoRaw)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();

        StringBuilder expandido = new StringBuilder(limpio);
        for (String p : limpio.split(" ")) {
            for (Map.Entry<String, String> entry : CONCEPTOS_SEMANTICOS.entrySet()) {
                if (p.startsWith(entry.getKey()) || (entry.getKey().startsWith(p) && p.length() >= 4)) {
                    expandido.append(" ").append(entry.getValue());
                }
            }
        }

        String[] tokens = expandido.toString().split("\\s+");
        Set<String> stopwords = Set.of("para", "como", "donde", "quiero", "hacer", "algo", "lugar", "sitio", "cartagena", "unos", "unas");

        for (int i = 0; i < tokens.length; i++) {
            String tok = tokens[i];
            if (tok.length() < 3 || stopwords.contains(tok)) continue;
            acumularRasgo(vec, "w:" + tok, 2.4f);
            if (tok.length() >= 5) acumularRasgo(vec, "stem:" + tok.substring(0, 5), 1.8f);
            if (i + 1 < tokens.length && tokens[i + 1].length() >= 3) {
                acumularRasgo(vec, "bi:" + tok + "_" + tokens[i + 1], 1.5f);
            }
            String padded = "<" + tok + ">";
            for (int j = 0; j <= padded.length() - 3; j++) {
                acumularRasgo(vec, "g3:" + padded.substring(j, j + 3), 0.55f);
            }
        }
        return normalizarL2(vec);
    }

    public static double similitudCoseno(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return 0.0;
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private void acumularRasgo(float[] vec, String feature, float peso) {
        int h = mezclarMurmur(feature.hashCode());
        int idx = Math.floorMod(h, LOCAL_VECTOR_DIM);
        float signo = ((h >>> 15) & 1) == 0 ? 1.0f : -1.0f;
        vec[idx] += signo * peso;
    }

    private static int mezclarMurmur(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    private static float[] normalizarL2(float[] vec) {
        double sum = 0.0;
        for (float v : vec) sum += (double) v * v;
        if (sum == 0.0) return vec;
        float inv = (float) (1.0 / Math.sqrt(sum));
        for (int i = 0; i < vec.length; i++) vec[i] *= inv;
        return vec;
    }

    private List<DocumentoCatalogo> construirDocumentosActivos() {
        List<DocumentoCatalogo> resultado = new ArrayList<>(CATALOGO_BASE_SEMANTICO);
        if (lugarRepository != null) {
            try {
                Set<String> yaIncluidos = new HashSet<>();
                CATALOGO_BASE_SEMANTICO.forEach(d -> yaIncluidos.add(GeminiAiService.normalizarTexto(d.nombre())));
                for (Lugar l : lugarRepository.findByEstado(Lugar.Estado.APROBADO)) {
                    String norm = GeminiAiService.normalizarTexto(l.getNombre());
                    if (yaIncluidos.add(norm)) {
                        resultado.add(new DocumentoCatalogo(
                                l.getId(), l.getNombre(),
                                l.getCategoria() != null ? l.getCategoria().getNombre() : "Turismo",
                                l.getUbicacion(), l.getDescripcion() != null ? l.getDescripcion() : "",
                                l.getImagenEfectiva(), ""
                        ));
                    }
                }
            } catch (Exception ignored) {}
        }
        return resultado;
    }

    private boolean tieneGeminiKeyValida() {
        return geminiApiKey != null && !geminiApiKey.isBlank()
                && !geminiApiKey.contains("TU_KEY") && !geminiApiKey.contains("tu_api_key");
    }

    private float[] obtenerEmbeddingGemini(String texto) {
        return cacheEmbeddingsGemini.computeIfAbsent(texto, k -> {
            try {
                String url = "https://generativelanguage.googleapis.com/v1beta/models/text-embedding-004:embedContent?key=" + geminiApiKey;
                Map<String, Object> body = Map.of(
                        "model", "models/text-embedding-004",
                        "content", Map.of("parts", List.of(Map.of("text", texto)))
                );
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(6))
                        .header("Content-Type", "application/json; charset=utf-8")
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (resp.statusCode() == 200) {
                    JsonNode values = objectMapper.readTree(resp.body()).path("embedding").path("values");
                    if (values.isArray() && !values.isEmpty()) {
                        float[] arr = new float[values.size()];
                        for (int i = 0; i < values.size(); i++) arr[i] = (float) values.get(i).asDouble();
                        return normalizarL2(arr);
                    }
                }
            } catch (Exception ex) {
                log.debug("Fallback a embedding local 256d: {}", ex.getMessage());
            }
            return null;
        });
    }
}
