package com.odyxs.vg.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import java.net.URI;
import java.util.Locale;

@Controller
public class IdiomaController {

    @Autowired
    private LocaleResolver localeResolver;

    @GetMapping("/idioma")
    public String cambiarIdioma(@RequestParam String lang,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        String langClean = "en".equalsIgnoreCase(lang) ? "en" : "es";
        localeResolver.setLocale(request, response, Locale.forLanguageTag(langClean));

        String destino = extraerRutaLocalSegura(request.getHeader("Referer"), request.getServerName());
        return "redirect:" + destino;
    }

    private String extraerRutaLocalSegura(String referer, String serverName) {
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = URI.create(referer.trim());
            if (uri.getHost() != null && !uri.getHost().equalsIgnoreCase(serverName)) {
                return "/";
            }
            String path = uri.getRawPath();
            if (path == null || path.isBlank() || !path.startsWith("/") || path.startsWith("//")) {
                return "/";
            }
            String query = uri.getRawQuery();
            return (query != null && !query.isBlank()) ? path + "?" + query : path;
        } catch (IllegalArgumentException ex) {
            return "/";
        }
    }
}
