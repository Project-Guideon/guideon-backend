package com.guideon.guideonbackend.domain.admin.service;

import com.guideon.guideonbackend.domain.admin.dto.AdminLoginRequest;
import com.guideon.guideonbackend.domain.admin.dto.AdminLoginResponse;
import com.guideon.guideonbackend.domain.admin.dto.AdminRefreshRequest;
import com.guideon.guideonbackend.domain.admin.dto.AdminRefreshResponse;
import com.guideon.guideonbackend.domain.admin.dto.AdminSignupRequest;
import com.guideon.guideonbackend.domain.admin.entity.Admin;
import com.guideon.guideonbackend.domain.admin.entity.AdminRole;
import com.guideon.guideonbackend.domain.admin.entity.RefreshToken;
import com.guideon.guideonbackend.domain.admin.repository.AdminRepository;
import com.guideon.guideonbackend.domain.admin.repository.RefreshTokenRepository;
import com.guideon.guideonbackend.global.exception.CustomException;
import com.guideon.guideonbackend.global.exception.ErrorCode;
import com.guideon.guideonbackend.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * 관리자 로그인
     *
     * @param request 로그인 요청 (이메일, 비밀번호)
     * @return AdminLoginResponse (Access Token, Refresh Token, 관리자 정보)
     */
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        // 이메일로 관리자 조회
        Admin admin = adminRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.AUTH_INVALID,
                        "이메일 또는 비밀번호가 일치하지 않습니다"
                ));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new CustomException(
                    ErrorCode.AUTH_INVALID,
                    "이메일 또는 비밀번호가 일치하지 않습니다"
            );
        }

        // 계정 활성화 확인
        if (!admin.getIsActive()) {
            throw new CustomException(
                    ErrorCode.AUTH_INVALID,
                    "비활성화된 계정입니다",
                    Map.of("reason", "account_deactivated")
            );
        }

        // 토큰 생성
        String accessToken = jwtProvider.createAccessToken(
                admin.getAdminId(),
                admin.getEmail(),
                admin.getRole()
        );
        String refreshToken = jwtProvider.createRefreshToken(admin.getAdminId());

        // Refresh Token 저장 (기존 토큰 자동 덮어쓰기)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .adminId(admin.getAdminId())
                .expiresAt(jwtProvider.calculateRefreshTokenExpiry())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 마지막 로그인 시각 갱신
        admin.updateLastLoginAt();

        return AdminLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .role(admin.getRole().name())
                .build();
    }

    /**
     * 토큰 갱신
     *
     * @param request 갱신 요청 (Refresh Token)
     * @return AdminRefreshResponse (새 Access Token, 새 Refresh Token)
     */
    @Transactional
    public AdminRefreshResponse refresh(AdminRefreshRequest request) {
        String refreshTokenValue = request.getRefreshToken();

        // Refresh Token 검증
        Long adminId;
        try {
            adminId = jwtProvider.getAdminId(refreshTokenValue);
            String tokenType = jwtProvider.getTokenType(refreshTokenValue);
            if (!"refresh".equals(tokenType)) {
                throw new CustomException(
                        ErrorCode.AUTH_INVALID,
                        "유효하지 않은 토큰 타입입니다"
                );
            }
        } catch (Exception e) {
            throw new CustomException(
                    ErrorCode.AUTH_INVALID,
                    "유효하지 않은 Refresh Token입니다"
            );
        }

        // Redis에서 Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.AUTH_INVALID,
                        "존재하지 않는 Refresh Token입니다"
                ));

        // 저장된 토큰과 요청 토큰 비교 (재사용 방지)
        if (!refreshTokenValue.equals(refreshToken.getToken())) {
            throw new CustomException(
                    ErrorCode.AUTH_INVALID,
                    "유효하지 않은 Refresh Token입니다"
            );
        }

        // 만료 확인
        if (refreshToken.isExpired()) {
            refreshTokenRepository.deleteById(adminId);
            throw new CustomException(
                    ErrorCode.AUTH_INVALID,
                    "만료된 Refresh Token입니다"
            );
        }

        // 관리자 조회
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "관리자를 찾을 수 없습니다"
                ));

        // 계정 활성화 확인
        if (!admin.getIsActive()) {
            throw new CustomException(
                    ErrorCode.AUTH_INVALID,
                    "비활성화된 계정입니다"
            );
        }

        // 새로운 토큰 생성
        String newAccessToken = jwtProvider.createAccessToken(
                admin.getAdminId(),
                admin.getEmail(),
                admin.getRole()
        );
        String newRefreshToken = jwtProvider.createRefreshToken(admin.getAdminId());

        // 새 Refresh Token 저장 (기존 토큰 자동 덮어쓰기)
        RefreshToken newRefreshTokenEntity = RefreshToken.builder()
                .token(newRefreshToken)
                .adminId(admin.getAdminId())
                .expiresAt(jwtProvider.calculateRefreshTokenExpiry())
                .build();
        refreshTokenRepository.save(newRefreshTokenEntity);

        return AdminRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

    /**
     * 관리자 회원가입
     * 테스트를 위한 임시 회원가입
     */
    @Transactional
    public AdminLoginResponse signup(AdminSignupRequest request) {
        // 이메일 중복 확인
        if (adminRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new CustomException(
                    ErrorCode.CONFLICT,
                    "이미 사용 중인 이메일입니다"
            );
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 관리자 생성
        Admin admin = Admin.builder()
                .email(request.getEmail().toLowerCase())
                .passwordHash(encodedPassword)
                .role(AdminRole.PLATFORM_ADMIN)
                .build();

        // DB에 저장
        adminRepository.save(admin);

        // 자동 로그인 처리 (토큰 생성)
        String accessToken = jwtProvider.createAccessToken(
                admin.getAdminId(),
                admin.getEmail(),
                admin.getRole()
        );
        String refreshToken = jwtProvider.createRefreshToken(admin.getAdminId());

        // Refresh Token 저장 (Redis)
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .token(refreshToken)
                .adminId(admin.getAdminId())
                .expiresAt(jwtProvider.calculateRefreshTokenExpiry())
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        // 마지막 로그인 시각 갱신
        admin.updateLastLoginAt();

        return AdminLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .adminId(admin.getAdminId())
                .email(admin.getEmail())
                .role(admin.getRole().name())
                .build();
    }

    //로그아웃
    @Transactional
    public void logout(Long adminId) {
        // Redis에서 Refresh Token 삭제 (PK=adminId)
        refreshTokenRepository.deleteById(adminId);
    }
}
