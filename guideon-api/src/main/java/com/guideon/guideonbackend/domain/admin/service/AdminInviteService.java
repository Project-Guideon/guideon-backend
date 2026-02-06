package com.guideon.guideonbackend.domain.admin.service;

import com.guideon.guideonbackend.domain.admin.dto.AcceptInviteRequest;
import com.guideon.guideonbackend.domain.admin.dto.AdminLoginResponse;
import com.guideon.guideonbackend.domain.admin.dto.CreateInviteRequest;
import com.guideon.guideonbackend.domain.admin.dto.InviteResponse;
import com.guideon.guideonbackend.domain.admin.entity.*;
import com.guideon.guideonbackend.domain.admin.repository.AdminInviteRepository;
import com.guideon.guideonbackend.domain.admin.repository.AdminRepository;
import com.guideon.guideonbackend.domain.admin.repository.AdminSiteRepository;
import com.guideon.guideonbackend.domain.admin.repository.RefreshTokenRepository;
import com.guideon.guideonbackend.domain.site.entity.Site;
import com.guideon.guideonbackend.domain.site.repository.SiteRepository;
import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.guideonbackend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInviteService {

    private final AdminInviteRepository adminInviteRepository;
    private final AdminRepository adminRepository;
    private final AdminSiteRepository adminSiteRepository;
    private final SiteRepository siteRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.invite.expire-days}")
    private int expireDays;

    @Transactional
    public InviteResponse createInvite(CreateInviteRequest request, Long createdByAdminId) {
        String email = request.getEmail().toLowerCase();

        // site 존재 확인
        Site site = siteRepository.findById(request.getSiteId())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "사이트를 찾을 수 없습니다"
                ));

        // 동일 site + email PENDING 초대 중복 체크
        if (adminInviteRepository.existsBySite_SiteIdAndEmailAndStatus(
                site.getSiteId(), email, AdminInviteStatus.PENDING)) {
            throw new CustomException(
                    ErrorCode.CONFLICT,
                    "해당 이메일로 이미 대기 중인 초대가 존재합니다"
            );
        }

        // UUID 토큰 생성 → SHA-256 해시
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        AdminInvite invite = AdminInvite.builder()
                .site(site)
                .email(email)
                .role(AdminRole.SITE_ADMIN)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(expireDays))
                .createdByAdminId(createdByAdminId)
                .build();

        adminInviteRepository.save(invite);
        log.info("초대 생성 완료: inviteId={}, siteId={}, email={}", invite.getInviteId(), site.getSiteId(), email);

        return InviteResponse.builder()
                .inviteId(invite.getInviteId())
                .siteId(site.getSiteId())
                .siteName(site.getName())
                .email(invite.getEmail())
                .role(invite.getRole().name())
                .status(invite.getStatus().name())
                .expiresAt(invite.getExpiresAt())
                .createdAt(invite.getCreatedAt())
                .token(rawToken) // 1회만 노출
                .build();
    }

    public List<InviteResponse> listInvites() {
        return adminInviteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(invite -> InviteResponse.builder()
                        .inviteId(invite.getInviteId())
                        .siteId(invite.getSite().getSiteId())
                        .siteName(invite.getSite().getName())
                        .email(invite.getEmail())
                        .role(invite.getRole().name())
                        .status(invite.getStatus().name())
                        .expiresAt(invite.getExpiresAt())
                        .createdAt(invite.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void expireInvite(Long inviteId) {
        AdminInvite invite = adminInviteRepository.findById(inviteId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "초대를 찾을 수 없습니다"
                ));

        if (invite.getStatus() != AdminInviteStatus.PENDING) {
            throw new CustomException(
                    ErrorCode.DOMAIN_RULE_VIOLATION,
                    "PENDING 상태의 초대만 만료 처리할 수 있습니다"
            );
        }

        invite.markExpired();
        log.info("초대 만료 처리: inviteId={}", inviteId);
    }

    @Transactional
    public AdminLoginResponse acceptInvite(String inviteToken, AcceptInviteRequest request) {
        String tokenHash = sha256(inviteToken);

        AdminInvite invite = adminInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "유효하지 않은 초대 토큰입니다"
                ));

        // 상태 확인
        if (invite.getStatus() != AdminInviteStatus.PENDING) {
            throw new CustomException(
                    ErrorCode.INVITE_ALREADY_USED,
                    "이미 사용되었거나 만료된 초대입니다"
            );
        }

        // 만료 확인
        if (invite.isExpired()) {
            invite.markExpired();
            throw new CustomException(
                    ErrorCode.INVITE_EXPIRED,
                    "만료된 초대입니다"
            );
        }

        // 이메일 중복 확인 (이미 계정이 있는 경우)
        if (adminRepository.existsByEmail(invite.getEmail())) {
            throw new CustomException(
                    ErrorCode.CONFLICT,
                    "이미 등록된 이메일입니다"
            );
        }

        // Admin 계정 생성
        Admin admin = Admin.builder()
                .email(invite.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(invite.getRole())
                .build();
        adminRepository.save(admin);

        // AdminSite 매핑 생성
        AdminSite adminSite = new AdminSite(admin, invite.getSite());
        adminSiteRepository.save(adminSite);

        // 초대 상태 업데이트
        invite.markUsed();

        // 자동 로그인 (토큰 발급)
        String accessToken = jwtProvider.createAccessToken(
                admin.getAdminId(),
                admin.getEmail(),
                admin.getRole()
        );
        String refreshToken = jwtProvider.createRefreshToken(admin.getAdminId());

        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .adminId(admin.getAdminId())
                .expiresAt(jwtProvider.calculateRefreshTokenExpiry())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        admin.updateLastLoginAt();

        log.info("초대 수락 완료: adminId={}, siteId={}, email={}",
                admin.getAdminId(), invite.getSite().getSiteId(), admin.getEmail());

        return AdminLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .role(admin.getRole().name())
                .siteIds(List.of(invite.getSite().getSiteId()))
                .build();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
