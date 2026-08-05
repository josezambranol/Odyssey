package com.odyxs.vg.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.LocaleResolver;

import java.util.Locale;

@Controller
public class IdiomaController {

    @Autowired
    private LocaleResolver localeResolver;

    @GetMapping("/idioma")
    public String cambiarIdioma(@RequestParam String lang,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        Locale locale = Locale.forLanguageTag(lang);
        localeResolver.setLocale(request, response, locale);

        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) referer = "/";
        return "redirect:" + referer;
    }
}
