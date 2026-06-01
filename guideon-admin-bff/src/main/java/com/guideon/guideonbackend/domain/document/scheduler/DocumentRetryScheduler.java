package com.guideon.guideonbackend.domain.document.scheduler;

import com.guideon.core.dto.document.DocumentDto;
import com.guideon.guideonbackend.client.CoreDocumentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PENDING 상태로 일정 시간 이상 방치된 문서를 자동으로 FastAPI에 재처리 요청합니다.
 * BFF 재시작 등으로 처리 요청이 유실된 경우를 자동 복구합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentRetryScheduler {

    private final CoreDocumentClient coreDocumentClient;

    @Value("${document.retry-pending-after-minutes:5}")
    private int retryAfterMinutes;

    @Scheduled(fixedDelayString = "${document.retry-interval-ms:300000}") // 기본 5분마다
    public void retryPendingDocuments() {
        List<DocumentDto> pendingDocs;
        try {
            pendingDocs = coreDocumentClient.getPendingDocuments(retryAfterMinutes);
        } catch (Exception e) {
            log.warn("[DocumentRetry] Core 조회 실패 (다음 주기에 재시도): {}", e.getMessage());
            return;
        }

        if (pendingDocs.isEmpty()) return;

        log.info("[DocumentRetry] PENDING 문서 {}개 재처리 요청", pendingDocs.size());

        for (DocumentDto doc : pendingDocs) {
            try {
                coreDocumentClient.reprocessDocument(doc.getSiteId(), doc.getDocId());
                log.info("[DocumentRetry] doc_id={}, site_id={}, name={} → 재처리 요청 완료",
                        doc.getDocId(), doc.getSiteId(), doc.getOriginalName());
            } catch (Exception e) {
                log.error("[DocumentRetry] doc_id={} 재처리 요청 실패: {}", doc.getDocId(), e.getMessage());
            }
        }
    }
}
