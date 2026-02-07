package com.guideon.core.api;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health Check 엔드포인트
 * Docker/Kubernetes 헬스체크 및 로드밸런서 사용
 *
 * Core가 독립 실행될 때만 활성화 (BFF에서 의존성으로 포함 시 비활성화)
 */
@RestController("coreHealthController")
@ConditionalOnProperty(name = "spring.application.name", havingValue = "guideon-core")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "guideon-core"
        ));
    }
}
