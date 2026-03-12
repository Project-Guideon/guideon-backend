package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.domain.device.repository.DeviceRepository;
import com.guideon.core.domain.mascot.entity.Mascot;
import com.guideon.core.domain.mascot.repository.MascotRepository;
import com.guideon.core.dto.kiosk.KioskBootstrapDto;
import com.guideon.core.dto.kiosk.KioskHeartbeatCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KioskService {

    private final DeviceRepository deviceRepository;
    private final MascotRepository mascotRepository;

    /**
     * 키오스크 부트스트랩 데이터 조회
     * device → site / zone / mascot 정보를 조합하여 반환
     */
    public KioskBootstrapDto getBootstrap(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        if (!device.getIsActive()) {
            throw new CustomException(ErrorCode.DEVICE_INACTIVE);
        }

        if (!device.getSite().getIsActive()) {
            throw new CustomException(ErrorCode.SITE_INACTIVE);
        }

        Optional<Mascot> mascotOpt = mascotRepository.findBySite_SiteId(device.getSite().getSiteId());

        KioskBootstrapDto.MascotSummary mascotSummary = mascotOpt
                .filter(Mascot::getIsActive)
                .map(m -> KioskBootstrapDto.MascotSummary.builder()
                        .name(m.getName())
                        .modelId(m.getModelId())
                        .defaultAnim(m.getDefaultAnim())
                        .greetingMsg(m.getGreetingMsg())
                        .ttsVoiceId(m.getTtsVoiceId())
                        .ttsVoiceJson(m.getTtsVoiceJson())
                        .build())
                .orElse(null);

        return KioskBootstrapDto.builder()
                .deviceId(device.getDeviceId())
                .siteId(device.getSite().getSiteId())
                .siteName(device.getSite().getName())
                .zoneId(device.getZone() != null ? device.getZone().getZoneId() : null)
                .zoneName(device.getZone() != null ? device.getZone().getName() : null)
                .mascot(mascotSummary)
                .build();
    }

    /**
     * 하트비트 처리: lastPing 갱신
     */
    @Transactional
    public void heartbeat(String deviceId, KioskHeartbeatCommand command) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        device.updateLastPing();
        log.debug("하트비트 수신: deviceId={}, version={}", deviceId, command.getVersion());
    }

    /**
     * 인증 성공 시각 갱신 (verify 호출 시)
     */
    @Transactional
    public void updateLastAuthAt(String deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.DEVICE_NOT_FOUND));

        device.updateLastAuthAt();
    }
}