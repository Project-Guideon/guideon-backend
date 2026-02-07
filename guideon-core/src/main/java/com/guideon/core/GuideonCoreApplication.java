package com.guideon.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Core Service 독립 실행용 Application
 * Docker 환경에서 Core가 별도 서비스로 실행될 때 사용
 */
@SpringBootApplication(scanBasePackages = {
        "com.guideon.core",
        "com.guideon.common"
})
public class GuideonCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuideonCoreApplication.class, args);
    }
}
