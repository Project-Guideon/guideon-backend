package com.guideon.core.domain.document.scheduler;

import com.guideon.core.domain.document.entity.DocStatus;
import com.guideon.core.domain.document.entity.Document;
import com.guideon.core.domain.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PENDING 상태로 일정 시간 이상 방치된 문서를 FAILED로 전환합니다.
 * FastAPI 처리 요청 유실(BFF 재시작 등) 시 무한 대기 방지용.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentTimeoutScheduler {

    private final DocumentRepository documentRepository;

    @Value("${document.pending-timeout-minutes:30}")
    private int pendingTimeoutMinutes;

    @Scheduled(fixedDelayString = "${document.timeout-check-interval-ms:300000}") // 기본 5분마다
    @Transactional
    public void failTimedOutDocuments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(pendingTimeoutMinutes);
        List<Document> timedOut = documentRepository.findByStatusAndCreatedAtBefore(DocStatus.PENDING, cutoff);

        if (timedOut.isEmpty()) return;

        for (Document doc : timedOut) {
            doc.updateStatus(DocStatus.FAILED, "처리 시간 초과 (PENDING " + pendingTimeoutMinutes + "분 이상)");
            log.warn("[DocumentTimeout] doc_id={}, site_id={}, name={} → FAILED ({}분 초과)",
                    doc.getDocId(), doc.getSite().getSiteId(), doc.getOriginalName(), pendingTimeoutMinutes);
        }
    }
}
