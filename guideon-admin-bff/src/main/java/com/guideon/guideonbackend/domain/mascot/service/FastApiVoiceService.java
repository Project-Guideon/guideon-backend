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

import java.util.Map;

/**
 * FastAPI 음성 클로닝 API 클라이언트.
 *
 * 흐름: Admin BFF → FastAPI POST /v1/voices/clone → Cartesia voices.clone()
 * 반환된 voice_id 를 tb_mascot.tts_voice_id 에 저장하면 마스코트 TTS에 적용됩니다.
 */
@Slf4j
@Service
public class FastApiVoiceService {

    private final String fastapiUrl;
    private final RestTemplate restTemplate;

    public FastApiVoiceService(
            @Value("${fastapi.service.url:http://localhost:8000}") String fastapiUrl,
            @Value("${fastapi.voice.connect-timeout:10000}") int connectTimeout,
            @Value("${fastapi.voice.read-timeout:120000}") int readTimeout) {
        this.fastapiUrl = fastapiUrl;

        // 음성 클로닝은 Cartesia 처리 시간이 있어 타임아웃을 넉넉히 설정
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        this.restTemplate = new RestTemplate(factory);

        log.info("FastApiVoiceService 초기화: url={}", fastapiUrl);
    }

    /**
     * 음성 샘플 파일을 FastAPI로 전달하여 Cartesia 커스텀 보이스를 생성합니다.
     *
     * @param audioFile 음성 샘플 파일 (WAV/MP3)
     * @param name      생성할 보이스 이름
     * @param language  ISO 639-1 언어 코드 (기본값: "ko")
     * @return 생성된 voice_id 와 name
     */
    public VoiceCloneResult cloneVoice(byte[] audioBytes, String originalFilename, String name, String language) {
        String url = fastapiUrl + "/v1/voices/clone";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        String filename = originalFilename != null ? originalFilename : "voice_sample.wav";

        ByteArrayResource resource = new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        body.add("file", resource);
        body.add("name", name);
        body.add("language", language != null ? language : "ko");

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.POST, request, Map.class);
            Map<String, Object> responseBody = response.getBody();

            if (responseBody == null) {
                throw new CustomException(ErrorCode.VOICE_CLONE_FAILED, "FastAPI 빈 응답");
            }

            String voiceId = (String) responseBody.get("voice_id");
            String voiceName = (String) responseBody.getOrDefault("name", name);

            if (voiceId == null || voiceId.isBlank()) {
                throw new CustomException(ErrorCode.VOICE_CLONE_FAILED, "응답에 voice_id 없음");
            }

            log.info("음성 클로닝 완료: voice_id={}, name={}", voiceId, voiceName);
            return new VoiceCloneResult(voiceId, voiceName);

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("FastAPI 음성 클로닝 실패: url={}", url, e);
            throw new CustomException(ErrorCode.VOICE_CLONE_FAILED, "음성 클로닝에 실패했습니다.");
        }
    }

    /**
     * Cartesia 보이스를 삭제합니다. Core 저장 실패 시 보상 처리용으로 호출됩니다.
     *
     * 삭제 실패는 과금 정합성 문제이므로 예외를 던지지 않고 에러 로그만 남깁니다.
     */
    public void deleteVoice(String voiceId) {
        String url = fastapiUrl + "/v1/voices/" + voiceId;
        try {
            restTemplate.delete(url);
            log.info("Cartesia 보이스 삭제 완료 (보상 처리): voice_id={}", voiceId);
        } catch (Exception e) {
            log.error("Cartesia 보이스 삭제 실패 — 수동 정리 필요: voice_id={}, error={}", voiceId, e.getMessage());
        }
    }

    public record VoiceCloneResult(String voiceId, String name) {}
}
