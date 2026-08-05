package com.odyxs.vg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.io.File;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver("ODYXS_LANG");
        clr.setDefaultLocale(new Locale("es"));
        clr.setCookieMaxAge(Duration.ofDays(365));
        return clr;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        lci.setParamName("lang");
        return lci;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // Carpeta de uploads externa al jar (persiste entre reinicios)
        // Se crea automáticamente en user.home/odyxs-uploads/
        String uploadsExterno = "file:" + System.getProperty("user.home") + "/odyxs-uploads/";

        // Rutas classpath para recursos estáticos empaquetados (img, css, js)
        String staticFs = "file:" + Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static").toAbsolutePath() + "/";
        String staticTarget = "file:" + Paths.get(System.getProperty("user.dir"),
                "target", "classes", "static").toAbsolutePath() + "/";

        // /uploads/** — primero busca en carpeta externa, luego en classpath (imágenes pre-cargadas)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                    uploadsExterno,
                    staticFs + "uploads/",
                    staticTarget + "uploads/",
                    "classpath:/static/uploads/"
                );

        // /img/**
        registry.addResourceHandler("/img/**")
                .addResourceLocations(
                    staticFs + "img/",
                    staticTarget + "img/",
                    "classpath:/static/img/"
                );

        // /css/**
        registry.addResourceHandler("/css/**")
                .addResourceLocations(
                    staticFs + "css/",
                    staticTarget + "css/",
                    "classpath:/static/css/"
                );

        // /js/**
        registry.addResourceHandler("/js/**")
                .addResourceLocations(
                    staticFs + "js/",
                    staticTarget + "js/",
                    "classpath:/static/js/"
                );
    }
}
