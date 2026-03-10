package com.guideon.core.api;

import com.guideon.core.dto.DailyInfoDto;
import com.guideon.core.dto.UpsertDailyInfoCommand;
import com.guideon.core.service.DailyInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/internal/v1/sites/{siteId}/daily-infos")
@RequiredArgsConstructor
public class DailyInfoInternalController {

    private final DailyInfoService dailyInfoService;

    /**
     * 날짜별 일일 정보 목록 조회
     * date 미입력 시 오늘 날짜 기준
     */
    @GetMapping
    public ResponseEntity<List<DailyInfoDto>> getDailyInfos(
            @PathVariable Long siteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        if (from != null && to != null) {
            return ResponseEntity.ok(dailyInfoService.getDailyInfosByDateRange(siteId, from, to));
        }
        return ResponseEntity.ok(dailyInfoService.getDailyInfosBySiteAndDate(siteId, date));
    }

    /**
     * 날짜 단위 upsert (Admin용)
     */
    @PutMapping("/{date}")
    public ResponseEntity<DailyInfoDto> upsert(
            @PathVariable Long siteId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody UpsertDailyInfoCommand command) {
        return ResponseEntity.ok(dailyInfoService.upsert(siteId, date, command));
    }

    /**
     * 일일 정보 삭제 (Admin용)
     */
    @DeleteMapping("/{dailyInfoId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long siteId,
            @PathVariable Long dailyInfoId) {
        dailyInfoService.delete(siteId, dailyInfoId);
        return ResponseEntity.noContent().build();
    }
}