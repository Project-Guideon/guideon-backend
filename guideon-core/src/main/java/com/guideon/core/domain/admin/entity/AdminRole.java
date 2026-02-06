package com.guideon.core.domain.admin.entity;

/**
 * 관리자 역할 구분
 * - PLATFORM_ADMIN: 플랫폼 전체 관리자 (모든 site 접근 가능)
 * - SITE_ADMIN: 관광지 운영자 (특정 site만 접근 가능, tb_admin_site로 관리)
 */
public enum AdminRole {
    /**
     * 플랫폼 전체 관리자
     * 모든 관광지(site)에 대한 접근 권한 보유
     */
    PLATFORM_ADMIN,

    /**
     * 관광지 운영자
     * tb_admin_site 테이블을 통해 특정 관광지에만 접근 가능
     */
    SITE_ADMIN
}
