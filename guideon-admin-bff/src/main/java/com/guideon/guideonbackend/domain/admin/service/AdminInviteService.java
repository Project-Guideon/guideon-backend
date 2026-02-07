package com.guideon.guideonbackend.domain.admin.service;

import com.guideon.core.domain.admin.entity.RefreshToken;
import com.guideon.core.domain.admin.repository.RefreshTokenRepository;
import com.guideon.core.dto.AcceptInviteCommand;
import com.guideon.core.dto.AcceptInviteResult;
import com.guideon.core.dto.CreateInviteCommand;
import com.guideon.core.dto.InviteDto;
import com.guideon.guideonbackend.client.CoreAdminClient;
import com.guideon.guideonbackend.domain.admin.dto.AcceptInviteRequest;
import com.guideon.guideonbackend.domain.admin.dto.AdminLoginResponse;
import com.guideon.guideonbackend.domain.admin.dto.CreateInviteRequest;
import com.guideon.guideonbackend.domain.admin.dto.InviteResponse;
import com.guideon.guideonbackend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Admin BFF Invite Service
 * Core Service를 Feign Client로 호출하고, JWT 발급은 BFF에서 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInviteService {

    private final CoreAdminClient coreAdminClient;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${app.invite.expire-days}")
    private int expireDays;

    /**
     * 초대 생성
     */
    public InviteResponse createInvite(CreateInviteRequest request, Long createdByAdminId) {
        CreateInviteCommand command = CreateInviteCommand.builder()
                .siteId(request.getSiteId())
                .email(request.getEmail())
                .createdByAdminId(createdByAdminId)
                .expireDays(expireDays)
                .build();

        InviteDto inviteDto = coreAdminClient.createInvite(command);
        log.info("초대 생성 완료: inviteId={}, siteId={}, email={}",
                inviteDto.getInviteId(), inviteDto.getSiteId(), inviteDto.getEmail());

        return InviteResponse.from(inviteDto);
    }

    /**
     * 초대 목록 조회
     */
    public List<InviteResponse> listInvites() {
        return coreAdminClient.listInvites().stream()
                .map(InviteResponse::from)
                .toList();
    }

    /**
     * 초대 만료 처리
     */
    public void expireInvite(Long inviteId) {
        coreAdminClient.expireInvite(inviteId);
        log.info("초대 만료 처리: inviteId={}", inviteId);
    }

    /**
     * 초대 수락 (계정 생성 + 자동 로그인)
     * Core에서 계정 생성 후, BFF에서 JWT 발급
     */
    public AdminLoginResponse acceptInvite(String inviteToken, AcceptInviteRequest request) {
        AcceptInviteCommand command = AcceptInviteCommand.builder()
                .inviteToken(inviteToken)
                .password(request.getPassword())
                .build();

        // Core에서 계정 생성
        AcceptInviteResult result = coreAdminClient.acceptInvite(command);

        // BFF에서 JWT 발급
        String accessToken = jwtProvider.createAccessToken(
                result.getAdminId(),
                result.getEmail(),
                result.getRole()
        );
        String refreshToken = jwtProvider.createRefreshToken(result.getAdminId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .adminId(result.getAdminId())
                .expiresAt(jwtProvider.calculateRefreshTokenExpiry())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("초대 수락 완료: adminId={}, email={}", result.getAdminId(), result.getEmail());

        return AdminLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .adminId(result.getAdminId())
                .email(result.getEmail())
                .role(result.getRole())
                .siteIds(result.getSiteIds())
                .build();
    }
}
