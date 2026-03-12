package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.mascot.entity.Mascot;
import com.guideon.core.domain.mascot.repository.MascotRepository;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.dto.mascot.CreateMascotCommand;
import com.guideon.core.dto.mascot.MascotDto;
import com.guideon.core.dto.mascot.UpdateMascotCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MascotService {

    private final MascotRepository mascotRepository;
    private final SiteRepository siteRepository;

    @Transactional
    public MascotDto createMascot(Long siteId, CreateMascotCommand command) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND,
                        "존재하지 않는 관광지입니다: " + siteId));

        if (!site.getIsActive()) {
            throw new CustomException(ErrorCode.SITE_INACTIVE);
        }

        if (mascotRepository.existsBySite_SiteId(siteId)) {
            throw new CustomException(ErrorCode.MASCOT_ALREADY_EXISTS);
        }

        Mascot mascot = Mascot.builder()
                .site(site)
                .name(command.getName())
                .modelId(command.getModelId())
                .defaultAnim(command.getDefaultAnim())
                .greetingMsg(command.getGreetingMsg())
                .systemPrompt(command.getSystemPrompt())
                .promptConfig(command.getPromptConfig())
                .ttsVoiceId(command.getTtsVoiceId())
                .ttsVoiceJson(command.getTtsVoiceJson())
                .imageUrl(command.getImageUrl())
                .build();

        Mascot saved = mascotRepository.save(mascot);
        log.info("마스코트 생성 완료: mascotId={}, siteId={}", saved.getMascotId(), siteId);
        return MascotDto.from(saved);
    }

    public MascotDto getMascot(Long siteId) {
        Mascot mascot = mascotRepository.findBySite_SiteId(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_NOT_FOUND));
        return MascotDto.from(mascot);
    }

    @Transactional
    public MascotDto updateMascot(Long siteId, UpdateMascotCommand command) {
        Mascot mascot = mascotRepository.findBySite_SiteId(siteId)
                .orElseThrow(() -> new CustomException(ErrorCode.MASCOT_NOT_FOUND));

        mascot.update(
                command.getName(), command.getModelId(), command.getDefaultAnim(),
                command.getGreetingMsg(), command.getSystemPrompt(), command.getPromptConfig(),
                command.getTtsVoiceId(), command.getTtsVoiceJson(), command.getImageUrl(),
                command.getActive()
        );

        log.info("마스코트 수정 완료: mascotId={}, siteId={}", mascot.getMascotId(), siteId);
        return MascotDto.from(mascot);
    }
}
