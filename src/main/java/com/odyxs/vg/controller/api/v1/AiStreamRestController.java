package com.odyxs.vg.controller.api.v1;

import com.odyxs.vg.service.GeminiAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "Streaming IA en Tiempo Real (SSE + Virtual Threads)",
     description = "Emisión progresiva de tokens con Server-Sent Events sobre hilos virtuales de Java 21 (Project Loom)")
public class AiStreamRestController {

    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    private GeminiAiService geminiAiService;

    @Operation(summary = "Streaming SSE de respuestas del Concierge IA (GET EventSource)")
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamGet(@RequestParam("q") String pregunta,
                                @RequestParam(value = "lang", required = false) String lang,
                                HttpServletRequest request) {
        return iniciarStreamSse(pregunta, lang, request.getRemoteAddr());
    }

    @Operation(summary = "Streaming SSE de respuestas del Concierge IA (POST)")
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamPost(@RequestBody Map<String, String> payload,
                                 HttpServletRequest request) {
        String pregunta = payload.getOrDefault("mensaje", "");
        String lang = payload.get("lang");
        return iniciarStreamSse(pregunta, lang, request.getRemoteAddr());
    }

    private SseEmitter iniciarStreamSse(String pregunta, String langParam, String clientIp) {
        SseEmitter emitter = new SseEmitter(45_000L);
        Locale locale = LocaleContextHolder.getLocale();
        boolean enIngles = "en".equalsIgnoreCase(langParam)
                || (langParam == null && "en".equalsIgnoreCase(locale.getLanguage()));

        long inicio = System.currentTimeMillis();
        AtomicBoolean primerTokenEnviado = new AtomicBoolean(false);
        final long[] ttftMs = {0L};

        VIRTUAL_EXECUTOR.submit(() -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("meta")
                        .data(Map.of(
                                "motor", geminiAiService.obtenerNombreMotorActivo(),
                                "virtualThread", Thread.currentThread().isVirtual()
                        )));

                GeminiAiService.ResultadoStreamIa resultado = geminiAiService.responderEnStream(
                        pregunta,
                        enIngles,
                        clientIp,
                        chunk -> {
                            if (primerTokenEnviado.compareAndSet(false, true)) {
                                ttftMs[0] = System.currentTimeMillis() - inicio;
                            }
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("token")
                                        .data(Map.of("chunk", chunk)));
                            } catch (IOException ioEx) {
                                throw new RuntimeException("Cliente cerró el stream SSE", ioEx);
                            }
                        }
                );

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(Map.of(
                                "motor", resultado.motor(),
                                "categoria", resultado.categoria(),
                                "ttftMs", ttftMs[0],
                                "duracionMs", resultado.duracionMs()
                        )));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }
}
