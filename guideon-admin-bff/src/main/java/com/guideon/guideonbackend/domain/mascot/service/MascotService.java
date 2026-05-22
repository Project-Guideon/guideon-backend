package com.guideon.guideonbackend.domain.mascot.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.dto.mascot.CreateMascotCommand;
import com.guideon.core.dto.mascot.MascotDto;
import com.guideon.core.dto.mascot.UpdateMascotCommand;
import com.guideon.guideonbackend.client.CoreMascotClient;
import com.guideon.guideonbackend.domain.mascot.dto.CreateMascotRequest;
import com.guideon.guideonbackend.domain.mascot.dto.MascotImageUploadResponse;
import com.guideon.guideonbackend.domain.mascot.dto.MascotResponse;
import com.guideon.guideonbackend.domain.mascot.dto.UpdateMascotRequest;
import com.guideon.guideonbackend.domain.mascot.dto.VoiceCloneResponse;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.guideonbackend.global.storage.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MascotService {

    private final CoreMascotClient coreMascotClient;
    private final FileStorageService fileStorageService;
    private final FastApiVoiceService fastApiVoiceService;

    public MascotResponse createMascot(Long siteId, CreateMascotRequest request,
                                       CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        CreateMascotCommand command = CreateMascotCommand.builder()
                .name(request.getName())
                .modelId(request.getModelId())
                .defaultAnim(request.getDefaultAnim())
                .greetingMsg(request.getGreetingMsg())
                .systemPrompt(request.getSystemPrompt())
                .promptConfig(request.getPromptConfig())
                .ttsVoiceId(request.getTtsVoiceId())
                .ttsVoiceJson(request.getTtsVoiceJson())
                .imageUrl(request.getImageUrl())
                .build();

        MascotDto dto = coreMascotClient.createMascot(siteId, command);
        log.info("마스코트 생성 완료: mascotId={}, siteId={}", dto.getMascotId(), siteId);
        return MascotResponse.from(dto);
    }

    public MascotResponse getMascot(Long siteId, CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);
        return MascotResponse.from(coreMascotClient.getMascot(siteId));
    }

    public MascotResponse updateMascot(Long siteId, UpdateMascotRequest request,
                                       CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        UpdateMascotCommand command = UpdateMascotCommand.builder()
                .name(request.getName())
                .modelId(request.getModelId())
                .defaultAnim(request.getDefaultAnim())
                .greetingMsg(request.getGreetingMsg())
                .systemPrompt(request.getSystemPrompt())
                .promptConfig(request.getPromptConfig())
                .ttsVoiceId(request.getTtsVoiceId())
                .ttsVoiceJson(request.getTtsVoiceJson())
                .imageUrl(request.getImageUrl())
                .active(request.getIsActive())
                .build();

        MascotDto dto = coreMascotClient.updateMascot(siteId, command);
        log.info("마스코트 수정 완료: mascotId={}, siteId={}", dto.getMascotId(), siteId);
        return MascotResponse.from(dto);
    }

    public MascotImageUploadResponse uploadMascotImage(Long siteId, MultipartFile file,
                                                         CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);
        FileValidator.validateImage(file);
        String fileHash = FileValidator.computeFileHash(file);
        String storageUrl = fileStorageService.store(siteId, fileHash, file);
        return MascotImageUploadResponse.builder()
                .imageUrl(storageUrl)
                .build();
    }

    /**
     * 음성 샘플로 Cartesia 커스텀 보이스를 생성하고, voice_id를 마스코트에 저장합니다.
     *
     * 흐름: Admin BFF → FastAPI /v1/voices/clone → Cartesia API → voice_id 반환
     *       → Core PATCH /mascot (ttsVoiceId 업데이트)
     */
    public VoiceCloneResponse cloneVoice(Long siteId, MultipartFile audioFile,
                                          String name, String language,
                                          CustomAdminDetails adminDetails) {
        validatePlatformAdmin(adminDetails);

        // Cartesia API 호출 전에 마스코트 존재 여부 확인 — 없으면 빠르게 실패
        coreMascotClient.getMascot(siteId);

        // 스트림은 한 번만 읽을 수 있으므로 바이트 배열로 먼저 읽어 검증과 전송에 재사용
        byte[] audioBytes;
        try {
            audioBytes = audioFile.getBytes();
        } catch (IOException e) {
            throw new CustomException(ErrorCode.VOICE_CLONE_FAILED, "음성 파일 읽기 실패");
        }
        FileValidator.validateAudio(audioFile.getOriginalFilename(), audioBytes, 10.0);

        // FastAPI 통해 Cartesia 보이스 클로닝
        FastApiVoiceService.VoiceCloneResult result =
                fastApiVoiceService.cloneVoice(audioBytes, audioFile.getOriginalFilename(), name, language);

        // 생성된 voice_id를 마스코트에 저장 — 실패 시 Cartesia 보이스 삭제(보상 처리)
        try {
            UpdateMascotCommand command = UpdateMascotCommand.builder()
                    .ttsVoiceId(result.voiceId())
                    .build();
            coreMascotClient.updateMascot(siteId, command);
        } catch (Exception e) {
            log.error("Core 마스코트 업데이트 실패, Cartesia 보이스 삭제 시도: siteId={}, voice_id={}", siteId, result.voiceId());
            fastApiVoiceService.deleteVoice(result.voiceId());
            throw e;
        }

        log.info("보이스 클로닝 완료 및 마스코트 저장: siteId={}, voice_id={}", siteId, result.voiceId());
        return VoiceCloneResponse.builder()
                .voiceId(result.voiceId())
                .name(result.name())
                .build();
    }

    private void validatePlatformAdmin(CustomAdminDetails adminDetails) {
        if (!AdminRole.PLATFORM_ADMIN.name().equals(adminDetails.getRole())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }
}
