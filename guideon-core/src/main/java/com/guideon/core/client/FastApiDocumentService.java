package com.guideon.core.client;

import com.guideon.core.dto.ProcessDocumentCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * FastAPI 비동기 호출 래퍼
 * Feign 인터페이스는 @Async를 직접 붙일 수 없어 별도 서비스로 분리
 * 실패 시 로그만 남기고 문서는 PENDING 유지 → 관리자가 재처리 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiDocumentService {

    private final FastApiDocumentClient fastApiDocumentClient;

    @Async
    public void processDocument(ProcessDocumentCommand command) {
        try {
            fastApiDocumentClient.processDocument(command);
            log.info("FastAPI 처리 요청 완료: docId={}", command.getDocId());
        } catch (Exception e) {
            log.error("FastAPI 처리 요청 실패 (문서는 PENDING 유지): docId={}, error={}",
                    command.getDocId(), e.getMessage());
        }
    }
}
