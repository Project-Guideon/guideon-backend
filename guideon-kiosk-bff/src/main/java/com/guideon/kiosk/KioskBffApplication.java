package com.guideon.kiosk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.guideon.kiosk",
        "com.guideon.core.domain"
})
@EntityScan(basePackages = "com.guideon.core.domain")
@EnableJpaRepositories(basePackages = "com.guideon.core.domain")
@EnableJpaAuditing
@EnableFeignClients(basePackages = "com.guideon.kiosk.client")
public class KioskBffApplication {

    public static void main(String[] args) {
        SpringApplication.run(KioskBffApplication.class, args);
    }
}