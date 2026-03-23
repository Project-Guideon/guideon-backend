package com.guideon.core.api;

import com.guideon.core.dto.admin.OperatorDto;
import com.guideon.core.dto.admin.UpdateOperatorCommand;
import com.guideon.core.service.OperatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/v1/sites/{siteId}/operators")
@RequiredArgsConstructor
public class OperatorInternalController {

    private final OperatorService operatorService;

    @GetMapping
    public ResponseEntity<List<OperatorDto>> getSiteOperators(@PathVariable Long siteId) {
        List<OperatorDto> operators = operatorService.getSiteOperators(siteId);
        return ResponseEntity.ok(operators);
    }

    @PatchMapping("/{operatorId}")
    public ResponseEntity<OperatorDto> updateOperatorStatus(
            @PathVariable Long siteId,
            @PathVariable Long operatorId,
            @RequestBody UpdateOperatorCommand command) {
        OperatorDto operator = operatorService.updateOperatorStatus(siteId, operatorId, command);
        return ResponseEntity.ok(operator);
    }
}
