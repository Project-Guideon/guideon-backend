package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * mesh-processor(Node.js :3001) 클라이언트.
 * Tripo rig GLB + 상태별 Mixamo 애니메이션 GLB → 통합 anim GLB 병합 요청.
 * admin-bff와 mesh-processor는 동일 볼륨(./uploads:/app/uploads)을 공유하므로
 * 파일시스템 경로를 직접 전달한다.
 */
@Slf4j
@Service
public class MeshProcessorClient {

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public MeshProcessorClient(
            @Value("${mesh-processor.url:http://mesh-processor:3001}") String baseUrl,
            @Value("${mesh-processor.connect-timeout:5000}") int connectTimeout,
            @Value("${mesh-processor.read-timeout:120000}") int readTimeout) {
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);

        log.info("MeshProcessorClient 초기화: url={}", baseUrl);
    }

    /**
     * GLB 병합 요청.
     *
     * @param baseGlbPath  Tripo 리깅 완료 GLB 로컬 경로 (예: /app/uploads/1/abc.glb)
     * @param animGlbs     { "Idle": "/app/uploads/1/def.glb", "Talking": "..." }
     *                     KEY = GLB 내부에 임베드될 Animation 클립명
     * @param outputPath   출력 GLB 로컬 경로 (예: /app/uploads/1/anim_42.glb)
     */
    @SuppressWarnings("unchecked")
    public void combine(String baseGlbPath, Map<String, String> animGlbs, String outputPath) {
        Map<String, Object> body = Map.of(
                "baseGlb",   baseGlbPath,
                "animGlbs",  animGlbs,
                "outputGlb", outputPath
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/combine",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("mesh-processor 병합 완료: outputGlb={}", outputPath);
            if (response != null && Boolean.FALSE.equals(response.get("success"))) {
                throw new CustomException(ErrorCode.UPSTREAM_TIMEOUT,
                        "mesh-processor 병합 실패: " + response.get("error"));
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.UPSTREAM_TIMEOUT,
                    "mesh-processor 호출 실패: " + e.getMessage());
        }
    }
}
