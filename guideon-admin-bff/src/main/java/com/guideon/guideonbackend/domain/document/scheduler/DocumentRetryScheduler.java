package com.guideon.guideonbackend.domain.document.scheduler;

import com.guideon.core.dto.document.DocumentDto;
import com.guideon.guideonbackend.client.CoreDocumentClient;
import feign.FeignException;
import feign.RetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * PENDING 상태로 일정 시간 이상 방치된 문서를 자동으로 FastAPI에 재처리 요청합니다.
 * BFF 재시작 등으로 처리 요청이 유실된 경우를 자동 복구합니다.
 * 다중 인스턴스 환경에서 중복 실행을 방지하기 위해 ShedLock을 사용합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentRetryScheduler {

    private final CoreDocumentClient coreDocumentClient;

    @Value("${document.retry-pending-after-minutes:5}")
    private int retryAfterMinutes;

    @Scheduled(fixedDelayString = "${document.retry-interval-ms:300000}")
    public void retryPendingDocuments() {
        List<DocumentDto> pendingDocs;
        try {
            pendingDocs = coreDocumentClient.getPendingDocuments(retryAfterMinutes);
        } catch (RetryableException | FeignException.ServiceUnavailable e) {
            log.warn("[DocumentRetry] Core 서비스 일시적 장애 (다음 주기에 재시도): {}", e.getMessage());
            return;
        } catch (FeignException e) {
            log.error("[DocumentRetry] Core 서비스 호출 실패 (status={}, 설정 확인 필요): {}", e.status(), e.getMessage());
            return;
        } catch (Exception e) {
            log.error("[DocumentRetry] 예상치 못한 오류 발생: ", e);
            return;
        }

        if (pendingDocs.isEmpty()) return;

        log.info("[DocumentRetry] PENDING 문서 {}개 재처리 요청", pendingDocs.size());

        for (DocumentDto doc : pendingDocs) {
            try {
                coreDocumentClient.reprocessDocument(doc.getSiteId(), doc.getDocId());
                log.info("[DocumentRetry] doc_id={}, site_id={}, name={} → 재처리 요청 완료",
                        doc.getDocId(), doc.getSiteId(), doc.getOriginalName());
            } catch (FeignException e) {
                log.error("[DocumentRetry] doc_id={} 재처리 요청 실패 (status={}): {}",
                        doc.getDocId(), e.status(), e.getMessage());
            } catch (Exception e) {
                log.error("[DocumentRetry] doc_id={} 재처리 요청 중 예상치 못한 오류: ", doc.getDocId(), e);
            }
        }
    }
}
