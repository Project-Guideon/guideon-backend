package com.guideon.core.domain.pairing.entity;

public enum PairingStatus {
    WAITING,  // 코드 발급 후 관리자 매칭 대기
    PAIRED,   // 관리자가 매칭 완료 → 키오스크에 토큰 전달 가능
    CLAIMED,  // 키오스크가 토큰 수령 완료 (재수령 불가)
    EXPIRED   // 만료
}
