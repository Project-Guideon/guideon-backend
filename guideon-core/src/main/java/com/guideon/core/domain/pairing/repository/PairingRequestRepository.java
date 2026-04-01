package com.guideon.core.domain.pairing.repository;

import com.guideon.core.domain.pairing.entity.PairingRequest;
import com.guideon.core.domain.pairing.entity.PairingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PairingRequestRepository extends JpaRepository<PairingRequest, Long> {

    Optional<PairingRequest> findByPairingCode(String pairingCode);

    Optional<PairingRequest> findByPairingCodeAndStatus(String pairingCode, PairingStatus status);

    boolean existsByPairingCodeAndStatus(String pairingCode, PairingStatus status);

    List<PairingRequest> findByStatusAndExpiresAtBefore(PairingStatus status, LocalDateTime now);
}
