package com.guideon.guideonbackend.global.init;

import com.guideon.core.domain.admin.entity.Admin;
import com.guideon.core.domain.admin.entity.AdminRole;
import com.guideon.core.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-email}")
    private String adminEmail;

    @Value("${app.init.admin-password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        String email = adminEmail.toLowerCase();

        if (adminRepository.existsByEmail(email)) {
            log.info("PLATFORM_ADMIN 시드 계정이 이미 존재합니다: {}", email);
            return;
        }

        Admin admin = Admin.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(AdminRole.PLATFORM_ADMIN)
                .build();

        adminRepository.save(admin);
        log.info("PLATFORM_ADMIN 시드 계정 생성 완료: {}", email);
    }
}
