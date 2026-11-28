package com.odyxs.vg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Servicio Criptográfico de Pases Turísticos QR Firmados con HMAC-SHA256 y Validación RNT (Odyssey v5.0).
 * Verifica autenticidad en tiempo constante (MessageDigest.isEqual) sin necesidad de consultar base de datos
 * y valida el formato de Registro Nacional de Turismo (Ley 2068 de 2020 Art. 31).
 */
@Service
public class QrPassCryptoService {

    private static final String HMAC_ALGO = "HmacSHA256";
    private static final String PREFIX = "ODY5";

    @Value("${odyssey.pass.hmac-secret:}")
    private String configuredSecret;

    private byte[] secretKeyBytes;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record SolicitudPaseRequest(
            String nombreViajero,
            String destinoActividad,
            int personas,
            long precioAcordadoCop,
            String numeroRnt
    ) {}

    public record PaseEmitidoDto(
            String codigoPase,
            String tokenFirmado,
            String firmaHexCorta,
            String estadoRnt,
            boolean rntFormatoValido,
            String nombreViajero,
            String destinoActividad,
            int personas,
            long precioAcordadoCop,
            String emitidoEn,
            String expiraEn,
            String svgVisualQr
    ) {}

    public record ResultadoVerificacionDto(
            boolean autentico,
            boolean vigente,
            String mensajeVeredicto,
            Map<String, Object> datosDecodificados
    ) {}

    @PostConstruct
    public void init() {
        if (configuredSecret != null && configuredSecret.trim().length() >= 32) {
            this.secretKeyBytes = configuredSecret.trim().getBytes(StandardCharsets.UTF_8);
        } else {
            byte[] rnd = new byte[32];
            new SecureRandom().nextBytes(rnd);
            this.secretKeyBytes = rnd;
        }
    }

    public PaseEmitidoDto emitirPaseFirmado(SolicitudPaseRequest req) {
        if (req == null || req.destinoActividad() == null || req.destinoActividad().isBlank()) {
            throw new IllegalArgumentException("El destino o actividad del pase es obligatorio.");
        }
        String viajero = (req.nombreViajero() != null && !req.nombreViajero().isBlank())
                ? req.nombreViajero().trim()
                : "Viajero Odyssey";
        String destino = req.destinoActividad().trim();
        int pax = Math.max(1, Math.min(req.personas(), 25));
        long precio = Math.max(0L, req.precioAcordadoCop());

        String rntLimpio = req.numeroRnt() != null ? req.numeroRnt().trim() : "";
        boolean rntValido = rntLimpio.matches("^\\d{5,8}$");
        String estadoRnt = rntLimpio.isEmpty()
                ? "PASE_CULTURAL_GRATUITO (Sin intermediación comercial)"
                : (rntValido ? "RNT_FORMATO_OFICIAL_LEY2068 (" + rntLimpio + ")" : "RNT_NO_VERIFICADO_REVISAR_MINCIT");

        Instant ahora = Instant.now();
        Instant expira = ahora.plus(7, ChronoUnit.DAYS);
        String codigo = "CTG-" + Long.toString(Math.abs(ahora.toEpochMilli() % 1_000_000L), 36).toUpperCase(Locale.ROOT)
                + "-" + (1000 + new SecureRandom().nextInt(9000));

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("cid", codigo);
        claims.put("sub", viajero);
        claims.put("dst", destino);
        claims.put("pax", pax);
        claims.put("cop", precio);
        claims.put("rnt", rntValido ? rntLimpio : "N/A");
        claims.put("iat", ahora.getEpochSecond());
        claims.put("exp", expira.getEpochSecond());

        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(claims);
            String payloadB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
            byte[] sigBytes = calcularHmac(payloadB64);
            String sigB64 = Base64.getUrlEncoder().withoutPadding().encodeToString(sigBytes);
            String tokenCompleto = PREFIX + "." + payloadB64 + "." + sigB64;
            String firmaCorta = HexFormat.of().formatHex(sigBytes).substring(0, 16).toUpperCase(Locale.ROOT);

            return new PaseEmitidoDto(
                    codigo,
                    tokenCompleto,
                    firmaCorta,
                    estadoRnt,
                    rntValido,
                    viajero,
                    destino,
                    pax,
                    precio,
                    ahora.toString(),
                    expira.toString(),
                    generarMatrizVisualSvg(tokenCompleto, codigo)
            );
        } catch (Exception e) {
            throw new IllegalStateException("Error firmando criptográficamente el pase QR.", e);
        }
    }

    public ResultadoVerificacionDto verificarPaseFirmado(String tokenRaw) {
        if (tokenRaw == null || tokenRaw.isBlank()) {
            return new ResultadoVerificacionDto(false, false, "Token vacío o ausente.", Map.of());
        }
        String[] partes = tokenRaw.trim().split("\\.");
        if (partes.length != 3 || !PREFIX.equals(partes[0])) {
            return new ResultadoVerificacionDto(false, false, "Estructura de token inválida (no corresponde a un Pase Odyssey v5.0).", Map.of());
        }

        try {
            byte[] firmaEsperada = calcularHmac(partes[1]);
            byte[] firmaRecibida = Base64.getUrlDecoder().decode(partes[2]);

            // Comparación en tiempo constante para mitigar ataques de temporización (Timing Attacks)
            if (!MessageDigest.isEqual(firmaEsperada, firmaRecibida)) {
                return new ResultadoVerificacionDto(false, false, "🚨 Firma HMAC-SHA256 inválida: el pase fue alterado o falsificado.", Map.of());
            }

            byte[] payloadBytes = Base64.getUrlDecoder().decode(partes[1]);
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payloadBytes, LinkedHashMap.class);

            long exp = claims.get("exp") instanceof Number n ? n.longValue() : 0L;
            boolean vigente = Instant.now().getEpochSecond() <= exp;

            String veredicto = vigente
                    ? "✅ Pase auténtico y vigente verificado criptográficamente (HMAC-SHA256)."
                    : "⚠️ La firma es auténtica, pero el pase turístico ya expiró.";

            return new ResultadoVerificacionDto(true, vigente, veredicto, claims);
        } catch (Exception ex) {
            return new ResultadoVerificacionDto(false, false, "Token corrupto o ilegible.", Map.of());
        }
    }

    private byte[] calcularHmac(String mensajeAscii) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(secretKeyBytes, HMAC_ALGO));
        return mac.doFinal(mensajeAscii.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genera un sello matricial vectorial SVG limpio y determinístico a partir del hash SHA-256 del token firmado.
     */
    private String generarMatrizVisualSvg(String token, String codigo) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            int grid = 15;
            int cell = 10;
            int pad = 12;
            int size = grid * cell + pad * 2;
            StringBuilder sb = new StringBuilder();
            sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 ").append(size).append(" ").append(size + 22)
              .append("\" width=\"180\" height=\"202\" role=\"img\" aria-label=\"Pase QR ").append(codigo).append("\">");
            sb.append("<rect width=\"").append(size).append("\" height=\"").append(size + 22).append("\" rx=\"12\" fill=\"#fffdf9\" stroke=\"#d4a64a\" stroke-width=\"2\"/>");

            dibujarFinder(sb, pad, pad, cell);
            dibujarFinder(sb, pad + (grid - 4) * cell, pad, cell);
            dibujarFinder(sb, pad, pad + (grid - 4) * cell, cell);

            int bitIdx = 0;
            for (int r = 0; r < grid; r++) {
                for (int c = 0; c < grid; c++) {
                    boolean enFinder = (r < 4 && c < 4) || (r < 4 && c >= grid - 4) || (r >= grid - 4 && c < 4);
                    if (enFinder) continue;
                    int byteVal = hash[(bitIdx / 8) % hash.length] & 0xFF;
                    boolean on = ((byteVal >>> (bitIdx % 8)) & 1) == 1;
                    bitIdx++;
                    if (on) {
                        sb.append("<rect x=\"").append(pad + c * cell).append("\" y=\"").append(pad + r * cell)
                          .append("\" width=\"").append(cell - 1).append("\" height=\"").append(cell - 1)
                          .append("\" rx=\"2\" fill=\"#071630\"/>");
                    }
                }
            }
            sb.append("<text x=\"").append(size / 2).append("\" y=\"").append(size + 14)
              .append("\" text-anchor=\"middle\" font-family=\"monospace\" font-size=\"10\" font-weight=\"700\" fill=\"#0b2d6b\">")
              .append(codigo).append("</text></svg>");
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private static void dibujarFinder(StringBuilder sb, int x, int y, int cell) {
        int outer = cell * 4 - 1;
        int inner = cell * 2;
        sb.append("<rect x=\"").append(x).append("\" y=\"").append(y).append("\" width=\"").append(outer)
          .append("\" height=\"").append(outer).append("\" rx=\"4\" fill=\"none\" stroke=\"#0b2d6b\" stroke-width=\"3\"/>");
        sb.append("<rect x=\"").append(x + cell).append("\" y=\"").append(y + cell).append("\" width=\"").append(inner - 1)
          .append("\" height=\"").append(inner - 1).append("\" rx=\"2\" fill=\"#d4a64a\"/>");
    }
}
