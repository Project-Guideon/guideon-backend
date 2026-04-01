package com.guideon.guideonbackend.domain.device.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.common.response.PageResponse;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.dto.device.CreateDeviceCommand;
import com.guideon.core.dto.device.DeviceDto;
import com.guideon.core.dto.device.RotateTokenResult;
import com.guideon.core.dto.device.UpdateDeviceCommand;
import com.guideon.core.dto.pairing.PairDeviceCommand;
import com.guideon.guideonbackend.client.CoreDeviceClient;
import com.guideon.guideonbackend.client.CorePairingClient;
import com.guideon.guideonbackend.domain.device.dto.*;
import com.guideon.guideonbackend.global.security.CustomAdminDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final CoreDeviceClient coreDeviceClient;
    private final CorePairingClient corePairingClient;
    private final AdminSiteRepository adminSiteRepository;

    public CreateDeviceResponse createDevice(Long siteId, CreateDeviceRequest request,
                                             CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        CreateDeviceCommand command = CreateDeviceCommand.builder()
                .deviceId(request.getDeviceId())
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .zoneSource(request.getZoneSource())
                .zoneId(request.getZoneId())
                .isActive(request.getIsActive())
                .build();

        RotateTokenResult result = coreDeviceClient.createDevice(siteId, command);
        log.info("디바이스 등록 완료: deviceId={}, siteId={}", result.getDevice().getDeviceId(), siteId);
        return CreateDeviceResponse.from(result);
    }

    public PageResponse<DeviceResponse> getDevices(Long siteId, Pageable pageable,
                                                    CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        PageResponse<DeviceDto> devicePage = coreDeviceClient.getDevices(
                siteId, pageable.getPageNumber(), pageable.getPageSize());

        return PageResponse.<DeviceResponse>builder()
                .items(devicePage.getItems().stream().map(DeviceResponse::from).toList())
                .page(devicePage.getPage())
                .build();
    }

    public DeviceResponse getDevice(Long siteId, String deviceId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        return DeviceResponse.from(coreDeviceClient.getDevice(siteId, deviceId));
    }

    public DeviceResponse updateDevice(Long siteId, String deviceId, UpdateDeviceRequest request,
                                       CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        UpdateDeviceCommand command = UpdateDeviceCommand.builder()
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(request.getIsActive())
                .zoneSource(request.getZoneSource())
                .zoneId(request.getZoneId())
                .build();

        DeviceDto dto = coreDeviceClient.updateDevice(siteId, deviceId, command);
        log.info("디바이스 수정 완료: deviceId={}, siteId={}", deviceId, siteId);
        return DeviceResponse.from(dto);
    }

    public void deleteDevice(Long siteId, String deviceId, CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        coreDeviceClient.deleteDevice(siteId, deviceId);
        log.info("디바이스 삭제 완료: deviceId={}, siteId={}", deviceId, siteId);
    }

    public RotateTokenResponse rotateToken(Long siteId, String deviceId,
                                           CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);
        RotateTokenResult result = coreDeviceClient.rotateToken(siteId, deviceId);
        log.info("디바이스 토큰 재발급 완료: deviceId={}, siteId={}", deviceId, siteId);
        return RotateTokenResponse.from(result);
    }

    public DeviceResponse pairDevice(Long siteId, PairDeviceRequest request,
                                     CustomAdminDetails adminDetails) {
        validateSiteAccess(adminDetails, siteId);

        PairDeviceCommand command = PairDeviceCommand.builder()
                .pairingCode(request.getPairingCode())
                .deviceId(request.getDeviceId())
                .locationName(request.getLocationName())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .zoneSource(request.getZoneSource())
                .zoneId(request.getZoneId())
                .build();

        DeviceDto dto = corePairingClient.pairDevice(siteId, command);
        log.info("페어링 매칭 완료: code={}, deviceId={}, siteId={}",
                request.getPairingCode(), request.getDeviceId(), siteId);
        return DeviceResponse.from(dto);
    }

    /**
     * 사이트 접근 권한 검증
     * PLATFORM_ADMIN: 모든 사이트 접근 가능
     * SITE_ADMIN: 본인에게 할당된 사이트만 접근 가능
     */
    private void validateSiteAccess(CustomAdminDetails adminDetails, Long siteId) {
        if (AdminRole.PLATFORM_ADMIN.name().equals(adminDetails.getRole())) {
            return;
        }
        if (AdminRole.SITE_ADMIN.name().equals(adminDetails.getRole())) {
            if (!adminSiteRepository.existsById_AdminIdAndId_SiteId(adminDetails.getAdminId(), siteId)) {
                throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
            }
            return;
        }
        throw new CustomException(ErrorCode.ADMIN_SITE_FORBIDDEN);
    }
}
