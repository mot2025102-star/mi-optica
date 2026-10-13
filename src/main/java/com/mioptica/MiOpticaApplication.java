package com.mioptica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MiOpticaApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiOpticaApplication.class, args);
    }
}