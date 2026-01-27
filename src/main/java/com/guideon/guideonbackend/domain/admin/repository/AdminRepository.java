package com.guideon.guideonbackend.domain.admin.repository;

import com.guideon.guideonbackend.domain.admin.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    //이메일로 관리자 조회
    Optional<Admin> findByEmail(String email);

    //회원가입 시 중복 체크용, 이메일 존재 여부 확인
    boolean existsByEmail(String email);
}
