package com.odyxs.vg.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    public boolean isSmtpConfigured() {
        return mailSender != null
                && mailUsername != null && !mailUsername.isBlank()
                && mailPassword != null && !mailPassword.isBlank();
    }

    /**
     * Envía un código OTP de 6 dígitos al correo asociado del usuario (Gmail / cualquier proveedor).
     * Retorna true si se despachó por SMTP real, false si se usó la bandeja local segura de desarrollo.
     */
    public boolean enviarCodigoOtp(String destinatario, String nombreUsuario, String codigo, String proposito) {
        String asunto = "🔐 Código de seguridad ODYSSEY: " + codigo + " — " + proposito;
        String html = construirPlantillaOtpHtml(nombreUsuario, codigo, proposito);
        return enviarCorreoHtml(destinatario, asunto, html);
    }

    /**
     * Envía confirmación por correo cuando un usuario se suscribe para recibir novedades de un sitio turístico.
     */
    public boolean enviarConfirmacionSuscripcion(String destinatario, String nombreUsuario, String nombreLugar, String ubicacion) {
        String asunto = "🔔 Suscripción activada: Novedades de " + nombreLugar + " — ODYSSEY";
        String html = construirPlantillaSuscripcionHtml(nombreUsuario, nombreLugar, ubicacion);
        return enviarCorreoHtml(destinatario, asunto, html);
    }

    private boolean enviarCorreoHtml(String destinatario, String asunto, String contenidoHtml) {
        if (!isSmtpConfigured()) {
            log.info("[ODYSSEY-MAIL] Servidor SMTP sin credenciales configuradas. Correo registrado para '{}': {}", destinatario, asunto);
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailUsername, "ODYSSEY — Cartagena de Indias");
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(contenidoHtml, true);
            mailSender.send(message);
            log.info("[ODYSSEY-MAIL] ✅ Correo enviado exitosamente vía SMTP a '{}'", destinatario);
            return true;
        } catch (Exception ex) {
            log.warn("[ODYSSEY-MAIL] ⚠️ No se pudo despachar por SMTP ({}): {}. Se activó bandeja de respaldo.", destinatario, ex.getMessage());
            return false;
        }
    }

    private String construirPlantillaOtpHtml(String nombre, String codigo, String proposito) {
        String saludo = (nombre != null && !nombre.isBlank()) ? "Hola, <strong>" + nombre + "</strong>:" : "Hola, viajero:";
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:580px;margin:0 auto;background:#fcfaf6;border-radius:18px;overflow:hidden;border:1px solid #e6dcc8;">
                <div style="background:linear-gradient(135deg,#071d49 0%%,#0b2d6b 100%%);padding:32px 28px;text-align:center;">
                    <div style="color:#d4a64a;font-size:12px;font-weight:700;letter-spacing:2px;text-transform:uppercase;margin-bottom:6px;">CARTAGENA DE INDIAS · PATRIMONIO UNESCO</div>
                    <h1 style="color:#ffffff;margin:0;font-size:26px;letter-spacing:1px;">ODYSSEY</h1>
                    <p style="color:rgba(255,255,255,0.75);margin:6px 0 0;font-size:14px;">Verificación de Seguridad &amp; Acceso</p>
                </div>
                <div style="padding:32px 28px;color:#1e293b;">
                    <p style="font-size:16px;margin-top:0;">%s</p>
                    <p style="font-size:15px;line-height:1.6;color:#475569;">Has solicitado un código único de verificación para: <strong>%s</strong>.</p>
                    <div style="margin:28px 0;padding:24px;background:#ffffff;border:2px dashed #c8963e;border-radius:14px;text-align:center;">
                        <div style="font-size:11px;text-transform:uppercase;letter-spacing:1.5px;color:#64748b;margin-bottom:8px;">TU CÓDIGO DE 6 DÍGITOS</div>
                        <div style="font-size:36px;font-weight:800;letter-spacing:8px;color:#0b2d6b;font-family:monospace;">%s</div>
                        <div style="font-size:12px;color:#94a3b8;margin-top:8px;">Válido durante los próximos 10 minutos</div>
                    </div>
                    <p style="font-size:13px;color:#64748b;line-height:1.5;">Si no realizaste esta solicitud, puedes ignorar este mensaje con total tranquilidad. Tu cuenta permanece protegida.</p>
                </div>
                <div style="background:#071d49;padding:18px 28px;text-align:center;color:rgba(255,255,255,0.65);font-size:12px;">
                    © 2026 ODYSSEY Cartagena de Indias · Guía Turística Inteligente
                </div>
            </div>
            """.formatted(saludo, proposito, codigo);
    }

    private String construirPlantillaSuscripcionHtml(String nombre, String nombreLugar, String ubicacion) {
        return """
            <div style="font-family:'Segoe UI',Arial,sans-serif;max-width:580px;margin:0 auto;background:#fcfaf6;border-radius:18px;overflow:hidden;border:1px solid #e6dcc8;">
                <div style="background:linear-gradient(135deg,#071d49 0%%,#0b2d6b 100%%);padding:32px 28px;text-align:center;">
                    <div style="color:#d4a64a;font-size:12px;font-weight:700;letter-spacing:2px;text-transform:uppercase;margin-bottom:6px;">ALERTAS CULTURALES &amp; TURÍSTICAS</div>
                    <h1 style="color:#ffffff;margin:0;font-size:26px;">🔔 Novedades Activadas</h1>
                </div>
                <div style="padding:32px 28px;color:#1e293b;">
                    <p style="font-size:16px;margin-top:0;">¡Hola, <strong>%s</strong>!</p>
                    <p style="font-size:15px;line-height:1.6;color:#475569;">Ahora recibirás notificaciones prioritarias sobre eventos especiales, cambios de horario, festivales y recomendaciones de:</p>
                    <div style="margin:22px 0;padding:20px;background:#ffffff;border-left:4px solid #c8963e;border-radius:10px;box-shadow:0 4px 14px rgba(11,45,107,0.06);">
                        <h3 style="margin:0 0 6px;color:#0b2d6b;font-size:18px;">📍 %s</h3>
                        <p style="margin:0;color:#64748b;font-size:13px;">%s · Cartagena de Indias</p>
                    </div>
                    <p style="font-size:13px;color:#64748b;">Puedes administrar tus sitios de interés en cualquier momento desde tu panel <strong>Mi Perfil</strong> en ODYSSEY.</p>
                </div>
            </div>
            """.formatted(nombre != null ? nombre : "Viajero", nombreLugar, ubicacion != null ? ubicacion : "Centro Histórico");
    }
}
