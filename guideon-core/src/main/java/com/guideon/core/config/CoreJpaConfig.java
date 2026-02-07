package com.guideon.core.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Core Service JPA Auditing 설정
 * Core가 독립 실행될 때만 활성화 (BFF에서 의존성으로 포함 시 비활성화)
 */
@Configuration
@EnableJpaAuditing
@ConditionalOnProperty(name = "spring.application.name", havingValue = "guideon-core")
public class CoreJpaConfig {
}
