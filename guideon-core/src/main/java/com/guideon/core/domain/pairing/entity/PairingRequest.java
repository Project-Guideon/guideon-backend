package com.guideon.core.domain.pairing.entity;

import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_pairing_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PairingRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pairing_id")
    private Long pairingId;

    @Column(name = "pairing_code", nullable = false, unique = true, length = 6)
    private String pairingCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PairingStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "paired_at")
    private LocalDateTime pairedAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Builder
    public PairingRequest(String pairingCode, LocalDateTime expiresAt) {
        this.pairingCode = pairingCode;
        this.status = PairingStatus.WAITING;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void pair(Device device) {
        this.device = device;
        this.status = PairingStatus.PAIRED;
        this.pairedAt = LocalDateTime.now();
    }

    public void claim() {
        this.status = PairingStatus.CLAIMED;
        this.claimedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = PairingStatus.EXPIRED;
    }
}
