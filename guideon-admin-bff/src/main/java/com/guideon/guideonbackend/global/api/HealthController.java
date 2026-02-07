package com.guideon.guideonbackend.global.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Health Check 엔드포인트
 * Docker/Kubernetes 헬스체크 및 로드밸런서 사용
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "guideon-admin-bff"
        ));
    }
}
