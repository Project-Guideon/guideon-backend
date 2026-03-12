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
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import com.guideon.guideonbackend.global.storage.FileStorageService;
import com.guideon.guideonbackend.global.storage.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class MascotService {

    private final CoreMascotClient coreMascotClient;
    private final FileStorageService fileStorageService;

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

    private void validatePlatformAdmin(CustomAdminDetails adminDetails) {
        if (!AdminRole.PLATFORM_ADMIN.name().equals(adminDetails.getRole())) {
            throw new CustomException(ErrorCode.ACCESS_DENIED);
        }
    }
}
