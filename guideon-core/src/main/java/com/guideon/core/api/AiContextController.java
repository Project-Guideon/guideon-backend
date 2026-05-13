package com.guideon.core.api;

import com.guideon.core.dto.place.NearbyPlaceResponse;
import com.guideon.core.dto.place.PlaceSearchResponse;
import com.guideon.core.service.PlaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * FastAPI → Spring Boot 콜백 API
 *
 * FastAPI LangGraph 가 처리 중 필요한 데이터를 Spring Boot 에 요청하는 엔드포인트.
 * 인증 없음 (내부망 전용)
 */
@RestController
@RequestMapping("/internal/v1")
@RequiredArgsConstructor
public class AiContextController {

    private final PlaceService placeService;

    /**
     * FastAPI fetch_places_node 가 호출하는 근처 장소 조회 API.
     *
     * intent_gate 가 추출한 category 로 필터링해 거리순으로 반환.
     * category 가 null 이면 전체 카테고리 반환.
     */
    @GetMapping("/places/nearby")
    public ResponseEntity<List<NearbyPlaceResponse>> getNearbyPlaces(
            @RequestParam Long siteId,
            @RequestParam String deviceId,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(placeService.getNearbyPlacesByCategory(siteId, deviceId, category));
    }

    /**
     * FastAPI navigation_node 가 호출하는 장소명 유사도 검색 API.
     *
     * pg_trgm similarity() 로 가장 유사한 장소 1개 반환.
     * 유사도가 threshold 미만이면 null 반환 → FastAPI가 nearest 로직으로 fallback.
     */
    @GetMapping("/places/search")
    public ResponseEntity<PlaceSearchResponse> searchPlaceByName(
            @RequestParam Long siteId,
            @RequestParam String q,
            @RequestParam(defaultValue = "0.3") double threshold
    ) {
        return ResponseEntity.ok(placeService.searchByName(siteId, q, threshold));
    }
}
