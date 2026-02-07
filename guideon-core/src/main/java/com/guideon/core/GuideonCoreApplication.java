package com.guideon.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication(scanBasePackages = {
        "com.guideon.core",
        "com.guideon.common"
})
@EnableJpaAuditing
public class GuideonCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideonCoreApplication.class, args);
    }
}
