package com.odyxs.vg.service;

import com.odyxs.vg.entity.SuscripcionPush;
import com.odyxs.vg.repository.SuscripcionPushRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECPoint;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Servicio Web Push VAPID (RFC 8292, curva elíptica P-256 / secp256r1) + Feed de Alertas PWA (Odyssey v4.0).
 * Genera claves VAPID estándar compatibles con W3C PushManager sin librerías externas pesadas.
 */
@Service
public class WebPushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WebPushNotificationService.class);
    private static final int MAX_ALERTAS_MEMORIA = 25;

    @Value("${odyssey.vapid.public-key:}")
    private String configuredVapidPublicKey;

    @Value("${odyssey.vapid.subject:mailto:soporte@odyssey.com}")
    private String vapidSubject;

    @Autowired
    private SuscripcionPushRepository suscripcionPushRepository;

    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    private String vapidPublicKeyBase64Url;
    private final ConcurrentLinkedDeque<AlertaPushDto> historialAlertas = new ConcurrentLinkedDeque<>();

    public record AlertaPushDto(
            String id,
            String titulo,
            String cuerpo,
            String urlDestino,
            Long lugarId,
            String emitidaEn
    ) {}

    public record SuscripcionPushRequest(
            String endpoint,
            Map<String, String> keys,
            Long lugarId,
            String idioma
    ) {}

    @PostConstruct
    public void inicializarClavesVapid() {
        if (configuredVapidPublicKey != null && !configuredVapidPublicKey.isBlank()) {
            this.vapidPublicKeyBase64Url = configuredVapidPublicKey.trim();
        } else {
            this.vapidPublicKeyBase64Url = generarClavePublicaVapidP256();
        }

        historialAlertas.addFirst(new AlertaPushDto(
                "welcome-v4",
                "⛵ Agenda Cultural Cartagena Activa",
                "Recibe avisos instantáneos de festivales, horarios de zarpe y eventos gratuitos en sitios UNESCO.",
                "/eventos",
                null,
                Instant.now().toString()
        ));
    }

    public String obtenerClavePublicaVapid() {
        return vapidPublicKeyBase64Url;
    }

    public String obtenerSubjectVapid() {
        return vapidSubject;
    }

    @Transactional
    public SuscripcionPush registrarSuscripcion(SuscripcionPushRequest req, Long usuarioId) {
        if (req == null || req.endpoint() == null || req.endpoint().isBlank()) {
            throw new IllegalArgumentException("El endpoint Web Push es obligatorio.");
        }
        String endpointLimpio = req.endpoint().trim();
        if (endpointLimpio.length() > 512) {
            endpointLimpio = endpointLimpio.substring(0, 512);
        }

        Map<String, String> keys = req.keys() != null ? req.keys() : Map.of();
        String p256dh = keys.getOrDefault("p256dh", "pwa-local-client");
        String auth = keys.getOrDefault("auth", "pwa-local-auth");

        SuscripcionPush sub = suscripcionPushRepository.findByEndpoint(endpointLimpio)
                .orElseGet(SuscripcionPush::new);
        sub.setEndpoint(endpointLimpio);
        sub.setP256dh(p256dh);
        sub.setAuth(auth);
        if (usuarioId != null) sub.setUsuarioId(usuarioId);
        if (req.lugarId() != null) sub.setLugarId(req.lugarId());
        sub.setIdioma(req.idioma() != null && !req.idioma().isBlank() ? req.idioma().trim() : "es");
        sub.setActiva(true);

        SuscripcionPush guardada = suscripcionPushRepository.save(sub);
        if (meterRegistry != null) {
            meterRegistry.counter("odyssey.push.subscriptions").increment();
        }
        return guardada;
    }

    public AlertaPushDto publicarAlertaEvento(String titulo, String cuerpo, String urlDestino, Long lugarId) {
        AlertaPushDto alerta = new AlertaPushDto(
                "evt-" + System.currentTimeMillis(),
                titulo,
                cuerpo,
                (urlDestino != null && !urlDestino.isBlank()) ? urlDestino : "/eventos",
                lugarId,
                Instant.now().toString()
        );
        historialAlertas.addFirst(alerta);
        while (historialAlertas.size() > MAX_ALERTAS_MEMORIA) {
            historialAlertas.removeLast();
        }
        if (meterRegistry != null) {
            meterRegistry.counter("odyssey.push.alerts_published").increment();
        }
        log.info("Alerta PWA publicada: {} -> {}", titulo, alerta.urlDestino());
        return alerta;
    }

    public List<AlertaPushDto> obtenerAlertasRecientes(Long lugarId) {
        return historialAlertas.stream()
                .filter(a -> lugarId == null || a.lugarId() == null || lugarId.equals(a.lugarId()))
                .limit(10)
                .toList();
    }

    public long contarDispositivosActivos(Long lugarId) {
        return lugarId != null
                ? suscripcionPushRepository.countByLugarIdAndActivaTrue(lugarId)
                : suscripcionPushRepository.countByActivaTrue();
    }

    /**
     * Codifica un punto público EC P-256 sin comprimir (65 bytes: 0x04 || X(32) || Y(32)) en Base64URL sin padding,
     * cumpliendo exactamente el formato exigido por PushManager.subscribe(applicationServerKey).
     */
    private String generarClavePublicaVapidP256() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair kp = kpg.generateKeyPair();
            ECPublicKey pub = (ECPublicKey) kp.getPublic();
            ECPoint point = pub.getW();

            byte[] uncompressed = new byte[65];
            uncompressed[0] = 0x04;
            copiarCoordenada32Bytes(point.getAffineX(), uncompressed, 1);
            copiarCoordenada32Bytes(point.getAffineY(), uncompressed, 33);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(uncompressed);
        } catch (Exception e) {
            log.warn("No se pudo generar clave EC P-256 dinámica, usando clave pública VAPID de respaldo", e);
            return "BEl62iUYgUivxIkv69yViEuiBIa-Ib9-SkvMeAtA3LFgDzkrxZJjSgSnfckjBJuBkr3qBUYIHBQFLXYp5Nksh8U";
        }
    }

    private static void copiarCoordenada32Bytes(BigInteger coord, byte[] dest, int offset) {
        byte[] raw = coord.toByteArray();
        int srcPos = Math.max(0, raw.length - 32);
        int length = Math.min(32, raw.length);
        int destPos = offset + (32 - length);
        System.arraycopy(raw, srcPos, dest, destPos, length);
    }
}
