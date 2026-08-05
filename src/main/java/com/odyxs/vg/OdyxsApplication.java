package com.odyxs.vg;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import com.odyxs.vg.service.UsuarioService;

@SpringBootApplication
public class OdyxsApplication {
    public static void main(String[] args) {
        SpringApplication.run(OdyxsApplication.class, args);
    }

    @Bean
    CommandLineRunner init(UsuarioService usuarioService) {
        return args -> usuarioService.inicializarAdmin();
    }
}
