package com.guideon.core.domain.place.repository;

import com.guideon.core.domain.place.entity.Place;
import com.guideon.core.domain.place.entity.ZoneSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByPlaceIdAndSite_SiteId(Long placeId, Long siteId);

    @Query(value = """
            SELECT p.* FROM tb_place p
            LEFT JOIN tb_zone z ON z.zone_id = p.zone_id
            WHERE p.site_id = :siteId
            AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (CAST(:category AS TEXT) IS NULL OR p.category = :category)
            AND (CAST(:zoneId AS BIGINT) IS NULL OR p.zone_id = :zoneId)
            AND (CAST(:isActive AS BOOLEAN) IS NULL OR p.is_active = :isActive)
            """,
            countQuery = """
            SELECT COUNT(*) FROM tb_place p
            WHERE p.site_id = :siteId
            AND (CAST(:keyword AS TEXT) IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (CAST(:category AS TEXT) IS NULL OR p.category = :category)
            AND (CAST(:zoneId AS BIGINT) IS NULL OR p.zone_id = :zoneId)
            AND (CAST(:isActive AS BOOLEAN) IS NULL OR p.is_active = :isActive)
            """,
            nativeQuery = true)
    Page<Place> findByFilters(@Param("siteId") Long siteId,
                              @Param("keyword") String keyword,
                              @Param("category") String category,
                              @Param("zoneId") Long zoneId,
                              @Param("isActive") Boolean isActive,
                              Pageable pageable);

    /**
     * Zone 우선순위 + ST_Distance 기반 근처 장소 검색.
     *
     * 같은 INNER zone(+ 하위 SUB zone) 장소를 우선 정렬하고,
     * 그 안에서 키오스크 좌표 기준 거리순으로 정렬.
     * innerZoneId가 NULL(OUTER)이면 전체 site에서 거리순만 적용.
     */

    /**
     * category 필터 + Zone 우선순위 + 거리순 근처 장소 조회.
     *
     * [정렬 기준]
     * 1순위 zonePriority: 같은 INNER zone (+ 하위 SUB zone) 이면 0, 외부이면 1
     *   - innerZoneId 가 NULL(OUTER 디바이스)이면 모두 1로 동일 취급 → 거리순만 적용
     * 2순위 distanceM: ST_Distance 로 계산한 디바이스 ↔ 장소 간 실제 거리(미터)
     *
     * [파라미터]
     * - category: NULL 이면 전체 카테고리 반환, 값이 있으면 해당 카테고리만 필터링
     * - limitCount: 반환 건수 상한 (예: 일반 20, fetch_places_node 30)
     */
    @Query(value = """
            SELECT p.place_id AS placeId,
                   p.name AS name,
                   p.category AS category,
                   p.description AS description,
                   p.image_url AS imageUrl,
                   ST_Y(CAST(p.location AS geometry)) AS latitude,
                   ST_X(CAST(p.location AS geometry)) AS longitude,
                   ST_Distance(p.location, ST_MakePoint(:lng, :lat)\\:\\:geography) AS distanceM,
                   CASE
                     WHEN CAST(:innerZoneId AS BIGINT) IS NOT NULL AND (
                       p.zone_id = :innerZoneId
                       OR p.zone_id IN (
                         SELECT z.zone_id FROM tb_zone z
                         WHERE z.parent_zone_id = :innerZoneId
                       )
                     ) THEN 0
                     ELSE 1
                   END AS zonePriority
            FROM tb_place p
            WHERE p.site_id = :siteId
              AND p.is_active = true
              AND (CAST(:category AS TEXT) IS NULL OR p.category = :category)
            ORDER BY zonePriority, distanceM ASC
            LIMIT :limitCount
            """, nativeQuery = true)
    List<NearbyPlaceProjection> findNearbyPlacesByCategory(
            @Param("siteId") Long siteId,
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("innerZoneId") Long innerZoneId,
            @Param("category") String category,
            @Param("limitCount") int limitCount
    );

    /**
     * pg_trgm 유사도 기반 장소명 검색 (FastAPI navigation_node 전용)
     *
     * similarity() 내림차순으로 가장 유사한 장소 1개 반환.
     * 유사도가 threshold 미만이면 서비스 레이어에서 null 처리.
     */
    @Query(value = """
            SELECT p.place_id AS placeId,
                   p.name AS name,
                   p.category AS category,
                   ST_Y(CAST(p.location AS geometry)) AS latitude,
                   ST_X(CAST(p.location AS geometry)) AS longitude,
                   similarity(p.name, :q) AS similarity
            FROM tb_place p
            WHERE p.site_id = :siteId
              AND p.is_active = true
            ORDER BY similarity(p.name, :q) DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<PlaceSearchProjection> findTopByNameSimilarity(
            @Param("siteId") Long siteId,
            @Param("q") String q
    );

    interface PlaceSearchProjection {
        Long getPlaceId();
        String getName();
        String getCategory();
        Double getLatitude();
        Double getLongitude();
        Double getSimilarity();
    }

    /**
     * 좌표가 포함된 Zone ID 조회 (AUTO zone 할당용)
     * 가장 작은 레벨(SUB 다음 INNER 다음 outer)의 Zone을 반환
     * 어떤 Zone에도 포함되지 않으면 null 반환 (OUTER)
     */
    @Query(value = """
            SELECT z.zone_id
            FROM tb_zone z
            WHERE z.site_id = :siteId
              AND ST_Covers(z.area_geometry, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326))
            ORDER BY z.level DESC
            LIMIT 1
            """, nativeQuery = true)
    Long findZoneIdByCoordinates(
            @Param("siteId") Long siteId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );

    /**
     * site 내 AUTO zone 할당된 Place 목록 조회 (재계산 대상)
     */
    List<Place> findBySite_SiteIdAndZoneSource(Long siteId, ZoneSource zoneSource);
}
