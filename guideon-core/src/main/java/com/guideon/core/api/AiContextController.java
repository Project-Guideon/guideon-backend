package com.guideon.core.api;

import com.guideon.core.domain.place.repository.NearbyPlaceProjection;
import com.guideon.core.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final ChatService chatService;

    /**
     * FastAPI fetch_places_node 가 호출하는 근처 장소 조회 API.
     *
     * intent_gate 가 추출한 category 로 필터링해 거리순으로 반환.
     * category 가 null 이면 전체 카테고리 반환.
     */
    @GetMapping("/places/nearby")
    public ResponseEntity<List<Map<String, Object>>> getNearbyPlaces(
            @RequestParam Long siteId,
            @RequestParam String deviceId,
            @RequestParam(required = false) String category
    ) {
        List<NearbyPlaceProjection> places = chatService.getNearbyPlacesByCategory(siteId, deviceId, category);

        List<Map<String, Object>> response = places.stream()
                .map(p -> Map.<String, Object>of(
                        "placeId",    p.getPlaceId(),
                        "name",       p.getName(),
                        "category",   p.getCategory(),
                        "description", p.getDescription() != null ? p.getDescription() : "",
                        "distanceM",  p.getDistanceM(),
                        "sameZone",   p.getZonePriority() != null && p.getZonePriority() == 0
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }
}
