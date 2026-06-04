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
    /**
     * 스켈레톤 제거 요청.
     *
     * @param riggedGlbPath  Tripo rig 결과 GLB 로컬 경로
     * @param outputFbxPath  출력 FBX 로컬 경로
     */
    /**
     * 포맷 변환 요청 (assimp 확장자 추론).
     * 주 용도: Tripo rig 결과 FBX → GLB 변환.
     *
     * @param inputPath  입력 파일 로컬 경로 (예: /app/uploads/1/rig_raw_42.fbx)
     * @param outputPath 출력 파일 로컬 경로 (예: /app/uploads/1/mascot.glb)
     */
    @SuppressWarnings("unchecked")
    public void convert(String inputPath, String outputPath) {
        Map<String, Object> body = Map.of(
                "input",  inputPath,
                "output", outputPath
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/convert",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("mesh-processor convert 완료: output={}", outputPath);
            if (response != null && Boolean.FALSE.equals(response.get("success"))) {
                throw new CustomException(ErrorCode.UPSTREAM_TIMEOUT,
                        "mesh-processor convert 실패: " + response.get("error"));
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.UPSTREAM_TIMEOUT,
                    "mesh-processor convert 호출 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    public void stripRig(String riggedGlbPath, String outputFbxPath) {
        Map<String, Object> body = Map.of(
                "riggedGlb", riggedGlbPath,
                "outputFbx", outputFbxPath
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            Map<String, Object> response = restTemplate.postForObject(
                    baseUrl + "/strip-rig",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("mesh-processor strip-rig 완료: outputFbx={}", outputFbxPath);
            if (response != null && Boolean.FALSE.equals(response.get("success"))) {
                throw new CustomException(ErrorCode.UPSTREAM_TIMEOUT,
                        "mesh-processor strip-rig 실패: " + response.get("error"));
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.UPSTREAM_TIMEOUT,
                    "mesh-processor strip-rig 호출 실패: " + e.getMessage());
        }
    }

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
