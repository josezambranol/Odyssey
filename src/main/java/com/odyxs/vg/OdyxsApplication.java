package com.odyxs.vg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OdyxsApplication {
    public static void main(String[] args) {
        SpringApplication.run(OdyxsApplication.class, args);
    }
}
