package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Tripo AI REST API 클라이언트.
 *
 * 파이프라인: 이미지 업로드 → image_to_model task → 폴링 → animate_rig task → 폴링 → GLB 다운로드
 */
@Slf4j
@Service
public class TripoApiService {

    private static final long MAX_MODEL_SIZE = 50L * 1024 * 1024; // 50MB

    private final String apiKey;
    private final String baseUrl;
    private final RestTemplate restTemplate;

    public TripoApiService(
            @Value("${tripo.api-key}") String apiKey,
            @Value("${tripo.base-url}") String baseUrl,
            @Value("${tripo.connect-timeout:10000}") int connectTimeout,
            @Value("${tripo.read-timeout:60000}") int readTimeout) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);

        log.info("TripoApiService 초기화: baseUrl={}", baseUrl);
    }

    /**
     * Step 1: 이미지 byte[]를 Tripo에 업로드 → file token 반환
     */
    public String uploadImage(byte[] imageBytes, String filename) {
        String url = baseUrl + "/upload";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
        body.add("file", resource);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        Map<String, Object> response = callTripo(url, HttpMethod.POST, request);
        Map<String, Object> data = extractData(response);
        String imageToken = (String) data.get("image_token");

        if (imageToken == null) {
            throw new CustomException(ErrorCode.TRIPO_API_ERROR, "이미지 업로드 응답에 image_token 없음");
        }

        log.info("Tripo 이미지 업로드 완료: token={}", imageToken.substring(0, Math.min(20, imageToken.length())) + "...");
        return imageToken;
    }

    /**
     * Step 2: image_to_model task 생성 → task_id 반환
     */
    public String createImageToModelTask(String imageToken) {
        String url = baseUrl + "/task";

        Map<String, Object> requestBody = Map.of(
                "type", "image_to_model",
                "file", Map.of(
                        "type", "image",
                        "file_token", imageToken
                )
        );

        Map<String, Object> response = postJson(url, requestBody);
        Map<String, Object> data = extractData(response);
        String taskId = (String) data.get("task_id");

        log.info("Tripo image_to_model task 생성: taskId={}", taskId);
        return taskId;
    }

    /**
     * Step 3: animate_rig task 생성 (3D 모델에 리깅 적용) → task_id 반환
     */
    public String createAnimateRigTask(String originalModelTaskId) {
        String url = baseUrl + "/task";

        Map<String, Object> requestBody = Map.of(
                "type", "animate_rig",
                "original_model_task_id", originalModelTaskId
        );

        Map<String, Object> response = postJson(url, requestBody);
        Map<String, Object> data = extractData(response);
        String taskId = (String) data.get("task_id");

        log.info("Tripo animate_rig task 생성: taskId={}", taskId);
        return taskId;
    }

    /**
     * Step 4: animate_retarget task 생성 (리깅된 모델 + 5클립 배열 → GLB 1개) → task_id 반환
     *
     * @param rigTaskId animate_rig 완료된 task_id
     * @param presets   Tripo preset 식별자 목록 ({@link MascotMotion#presetList()}, 최대 5개)
     */
    public String createAnimateRetargetTask(String rigTaskId, List<String> presets) {
        String url = baseUrl + "/task";

        Map<String, Object> requestBody = Map.of(
                "type", "animate_retarget",
                "original_model_task_id", rigTaskId,
                "out_format", "glb",
                "animations", presets
        );

        Map<String, Object> response = postJson(url, requestBody);
        Map<String, Object> data = extractData(response);
        String taskId = (String) data.get("task_id");

        log.info("Tripo animate_retarget task 생성: presets={}, taskId={}", presets, taskId);
        return taskId;
    }

    /**
     * Task 상태 조회. 반환: {status, output{model}} 등
     */
    public TripoTaskStatus getTaskStatus(String taskId) {
        String url = baseUrl + "/task/" + taskId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        Map<String, Object> response = callTripo(url, HttpMethod.GET, request);
        Map<String, Object> data = extractData(response);

        String status = (String) data.get("status");
        String modelUrl = null;

        if ("success".equals(status)) {
            Map<String, Object> output = (Map<String, Object>) data.get("output");
            if (output != null) {
                // pbr_model이 있으면 우선, 없으면 model
                modelUrl = (String) output.get("pbr_model");
                if (modelUrl == null) {
                    modelUrl = (String) output.get("model");
                }
            }
        }

        return new TripoTaskStatus(status, modelUrl);
    }

    /**
     * GLB 모델 파일 다운로드 → byte[]
     */
    public byte[] downloadModel(String modelDownloadUrl) {
        try {
            // HEAD 요청으로 파일 크기 사전 확인
            HttpHeaders headHeaders = restTemplate.headForHeaders(modelDownloadUrl);
            long contentLength = headHeaders.getContentLength();
            if (contentLength > MAX_MODEL_SIZE) {
                throw new CustomException(ErrorCode.TRIPO_API_ERROR,
                        "모델 파일이 너무 큽니다: " + contentLength + " bytes (최대 " + MAX_MODEL_SIZE + ")");
            }

            ResponseEntity<byte[]> response = restTemplate.getForEntity(modelDownloadUrl, byte[].class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                if (response.getBody().length > MAX_MODEL_SIZE) {
                    throw new CustomException(ErrorCode.TRIPO_API_ERROR,
                            "모델 파일이 너무 큽니다: " + response.getBody().length + " bytes");
                }
                log.info("GLB 모델 다운로드 완료: size={}bytes", response.getBody().length);
                return response.getBody();
            }
            throw new CustomException(ErrorCode.TRIPO_API_ERROR, "모델 다운로드 실패: status=" + response.getStatusCode());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.TRIPO_API_ERROR, "모델 다운로드 실패: " + e.getMessage());
        }
    }

    // ── 내부 헬퍼 ──

    private Map<String, Object> postJson(String url, Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        return callTripo(url, HttpMethod.POST, request);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callTripo(String url, HttpMethod method, HttpEntity<?> request) {
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, method, request, Map.class);
            Map<String, Object> body = response.getBody();

            if (body == null) {
                throw new CustomException(ErrorCode.TRIPO_API_ERROR, "Tripo API 빈 응답");
            }

            Integer code = (Integer) body.get("code");
            if (code != null && code != 0) {
                String message = (String) body.getOrDefault("message", "알 수 없는 오류");
                log.error("Tripo API 비즈니스 오류: url={}, code={}, message={}", url, code, message);
                throw new CustomException(ErrorCode.TRIPO_API_ERROR, "Tripo API 오류 (code=" + code + "): " + message);
            }

            return body;
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tripo API 호출 실패: url={}, error={}", url, e.getMessage());
            throw new CustomException(ErrorCode.TRIPO_API_ERROR, "Tripo API 호출 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractData(Map<String, Object> response) {
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            throw new CustomException(ErrorCode.TRIPO_API_ERROR, "Tripo API 응답에 data 필드 없음");
        }
        return data;
    }

    public record TripoTaskStatus(String status, String modelUrl) {
        public boolean isSuccess() { return "success".equals(status); }
        public boolean isFailed() { return "failed".equals(status); }
        public boolean isProcessing() { return !isSuccess() && !isFailed(); }
    }
}
