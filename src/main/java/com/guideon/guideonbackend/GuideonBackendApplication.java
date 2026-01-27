package com.guideon.guideonbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GuideonBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideonBackendApplication.class, args);
    }

}