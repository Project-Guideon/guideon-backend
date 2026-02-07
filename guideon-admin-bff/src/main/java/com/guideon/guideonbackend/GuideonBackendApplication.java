package com.guideon.guideonbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.guideon.guideonbackend",
        "com.guideon.core.domain"
})
@EntityScan(basePackages = "com.guideon.core.domain")
@EnableJpaRepositories(basePackages = "com.guideon.core.domain")
@EnableRedisRepositories(basePackages = "com.guideon.core.domain")
@EnableJpaAuditing
@EnableFeignClients(basePackages = "com.guideon.guideonbackend.client")
public class GuideonBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideonBackendApplication.class, args);
    }

}