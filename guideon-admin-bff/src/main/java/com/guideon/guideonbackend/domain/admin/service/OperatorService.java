package com.guideon.guideonbackend.domain.admin.service;

import com.guideon.core.dto.admin.OperatorDto;
import com.guideon.core.dto.admin.UpdateOperatorCommand;
import com.guideon.guideonbackend.client.CoreOperatorClient;
import com.guideon.guideonbackend.domain.admin.dto.OperatorResponse;
import com.guideon.guideonbackend.domain.admin.dto.UpdateOperatorRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperatorService {

    private final CoreOperatorClient coreOperatorClient;

    public List<OperatorResponse> getSiteOperators(Long siteId) {
        List<OperatorDto> operators = coreOperatorClient.getSiteOperators(siteId);
        log.info("사이트 운영자 목록 조회: siteId={}, count={}", siteId, operators.size());
        return operators.stream().map(OperatorResponse::from).toList();
    }

    public OperatorResponse updateOperatorStatus(Long siteId, Long operatorId, UpdateOperatorRequest request) {
        UpdateOperatorCommand command = UpdateOperatorCommand.builder()
                .isActive(request.getIsActive())
                .build();

        OperatorDto operator = coreOperatorClient.updateOperatorStatus(siteId, operatorId, command);
        log.info("운영자 상태 변경: operatorId={}, siteId={}, isActive={}", operatorId, siteId, operator.getIsActive());
        return OperatorResponse.from(operator);
    }
}
