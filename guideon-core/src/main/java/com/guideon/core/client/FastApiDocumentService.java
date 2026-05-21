package com.guideon.core.client;

import com.guideon.core.dto.document.ProcessDocumentCommand;
import com.guideon.core.dto.document.UpdateDocumentStatusCommand;
import com.guideon.core.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * FastAPI 비동기 호출 래퍼
 * Feign 인터페이스는 @Async를 직접 붙일 수 없어 별도 서비스로 분리
 * 실패 시 상태를 FAILED로 업데이트하여 무한 PENDING 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiDocumentService {

    private final FastApiDocumentClient fastApiDocumentClient;
    private final DocumentService documentService;

    @Async("fastApiTaskExecutor")
    public void processDocument(ProcessDocumentCommand command) {
        try {
            fastApiDocumentClient.processDocument(command);
            log.info("FastAPI 처리 요청 완료: docId={}", command.getDocId());
        } catch (Exception e) {
            log.error("FastAPI 처리 요청 실패 → FAILED 처리: docId={}, error={}",
                    command.getDocId(), e.getMessage());
            try {
                documentService.updateDocumentStatus(
                        command.getSiteId(),
                        command.getDocId(),
                        UpdateDocumentStatusCommand.builder()
                                .status("FAILED")
                                .failedReason("FastAPI 연결 실패: " + e.getMessage())
                                .build()
                );
            } catch (Exception ex) {
                log.error("FAILED 상태 업데이트도 실패: docId={}, error={}", command.getDocId(), ex.getMessage());
            }
        }
    }
}
