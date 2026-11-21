package com.odyxs.vg.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${odyssey.upload.dir:${user.home}/odyssey-uploads}")
    private String uploadDir;

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver clr = new CookieLocaleResolver("ODYSSEY_LANG");
        clr.setDefaultLocale(Locale.of("es"));
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

        String cleanDir = (uploadDir != null && !uploadDir.isBlank())
                ? uploadDir.trim()
                : System.getProperty("user.home") + "/odyssey-uploads";
        if (!cleanDir.endsWith("/") && !cleanDir.endsWith("\\")) {
            cleanDir += "/";
        }
        String uploadsExterno = Paths.get(cleanDir).toAbsolutePath().toUri().toString();

        // Rutas classpath para recursos estáticos empaquetados (img, css, js)
        String staticFs = "file:" + Paths.get(System.getProperty("user.dir"),
                "src", "main", "resources", "static").toAbsolutePath() + "/";
        String staticTarget = "file:" + Paths.get(System.getProperty("user.dir"),
                "target", "classes", "static").toAbsolutePath() + "/";

        CacheControl cacheStatic = CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic();
        CacheControl cacheUploads = CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic();

        // /uploads/** — primero busca en carpeta externa, luego en classpath (imágenes pre-cargadas)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                    uploadsExterno,
                    staticFs + "uploads/",
                    staticTarget + "uploads/",
                    "classpath:/static/uploads/"
                )
                .setCacheControl(cacheUploads);

        // /img/**
        registry.addResourceHandler("/img/**")
                .addResourceLocations(
                    staticFs + "img/",
                    staticTarget + "img/",
                    "classpath:/static/img/"
                )
                .setCacheControl(cacheStatic);

        // /css/**
        registry.addResourceHandler("/css/**")
                .addResourceLocations(
                    staticFs + "css/",
                    staticTarget + "css/",
                    "classpath:/static/css/"
                )
                .setCacheControl(cacheStatic);

        // /js/**
        registry.addResourceHandler("/js/**")
                .addResourceLocations(
                    staticFs + "js/",
                    staticTarget + "js/",
                    "classpath:/static/js/"
                )
                .setCacheControl(cacheStatic);
    }
}
