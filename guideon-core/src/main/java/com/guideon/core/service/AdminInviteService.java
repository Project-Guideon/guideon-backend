package com.guideon.core.service;

import com.guideon.common.exception.CustomException;
import com.guideon.common.exception.ErrorCode;
import com.guideon.core.domain.admin.entity.*;
import com.guideon.core.domain.admin.repository.AdminInviteRepository;
import com.guideon.core.domain.admin.repository.AdminRepository;
import com.guideon.core.domain.admin.repository.AdminSiteRepository;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.site.repository.SiteRepository;
import com.guideon.core.dto.AcceptInviteCommand;
import com.guideon.core.dto.AcceptInviteResult;
import com.guideon.core.dto.CreateInviteCommand;
import com.guideon.core.dto.InviteDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Core Admin Invite Service - 순수 비즈니스 로직
 * JWT 발급은 BFF에서 처리
 */
@Slf4j
@Service("coreAdminInviteService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminInviteService {

    private final AdminInviteRepository adminInviteRepository;
    private final AdminRepository adminRepository;
    private final AdminSiteRepository adminSiteRepository;
    private final SiteRepository siteRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 초대 생성
     */
    @Transactional
    public InviteDto createInvite(CreateInviteCommand command) {
        String email = command.getEmail().toLowerCase();

        Site site = siteRepository.findById(command.getSiteId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "사이트를 찾을 수 없습니다"));

        // 동일 site + email PENDING 초대 중복 체크
        if (adminInviteRepository.existsBySite_SiteIdAndEmailAndStatus(
                site.getSiteId(), email, AdminInviteStatus.PENDING)) {
            throw new CustomException(ErrorCode.CONFLICT, "해당 이메일로 이미 대기 중인 초대가 존재합니다");
        }

        // UUID 토큰 생성 → SHA-256 해시
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        AdminInvite invite = AdminInvite.builder()
                .site(site)
                .email(email)
                .role(AdminRole.SITE_ADMIN)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusDays(command.getExpireDays()))
                .createdByAdminId(command.getCreatedByAdminId())
                .build();

        adminInviteRepository.save(invite);
        log.info("초대 생성 완료: inviteId={}, siteId={}", invite.getInviteId(), site.getSiteId());

        return InviteDto.fromWithToken(invite, rawToken);
    }

    /**
     * 초대 목록 조회
     */
    public List<InviteDto> listInvites() {
        return adminInviteRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(InviteDto::from)
                .toList();
    }

    /**
     * 초대 만료 처리
     */
    @Transactional
    public void expireInvite(Long inviteId) {
        AdminInvite invite = adminInviteRepository.findById(inviteId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "초대를 찾을 수 없습니다"));

        if (invite.getStatus() != AdminInviteStatus.PENDING) {
            throw new CustomException(ErrorCode.DOMAIN_RULE_VIOLATION, "PENDING 상태의 초대만 만료 처리할 수 있습니다");
        }

        invite.markExpired();
        log.info("초대 만료 처리: inviteId={}", inviteId);
    }

    /**
     * 초대 수락 (계정 생성)
     * JWT 발급은 BFF에서 처리
     */
    @Transactional
    public AcceptInviteResult acceptInvite(AcceptInviteCommand command) {
        String tokenHash = sha256(command.getInviteToken());

        AdminInvite invite = adminInviteRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "유효하지 않은 초대 토큰입니다"));

        // 상태 확인
        if (invite.getStatus() != AdminInviteStatus.PENDING) {
            throw new CustomException(ErrorCode.INVITE_ALREADY_USED, "이미 사용되었거나 만료된 초대입니다");
        }

        // 만료 확인
        if (invite.isExpired()) {
            invite.markExpired();
            throw new CustomException(ErrorCode.INVITE_EXPIRED, "만료된 초대입니다");
        }

        // 이메일 중복 확인
        if (adminRepository.existsByEmail(invite.getEmail())) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 등록된 이메일입니다");
        }

        // Admin 계정 생성
        Admin admin = Admin.builder()
                .email(invite.getEmail())
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .role(invite.getRole())
                .build();
        adminRepository.save(admin);

        // AdminSite 매핑 생성
        AdminSite adminSite = new AdminSite(admin, invite.getSite());
        adminSiteRepository.save(adminSite);

        // 초대 상태 업데이트
        invite.markUsed();

        // 마지막 로그인 시간 업데이트
        admin.updateLastLoginAt();

        log.info("초대 수락 완료: adminId={}, siteId={}",
                admin.getAdminId(), invite.getSite().getSiteId());

        return AcceptInviteResult.builder()
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
