package com.guideon.core.domain.place.repository;

import com.guideon.core.domain.place.entity.Place;
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
    @Query(value = """
            SELECT p.place_id AS placeId,
                   p.name AS name,
                   p.category AS category,
                   p.description AS description,
                   p.image_url AS imageUrl,
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
            ORDER BY zonePriority, distanceM ASC
            LIMIT :limitCount
            """, nativeQuery = true)
    List<NearbyPlaceProjection> findNearbyPlaces(
            @Param("siteId") Long siteId,
            @Param("lat") Double latitude,
            @Param("lng") Double longitude,
            @Param("innerZoneId") Long innerZoneId,
            @Param("limitCount") int limitCount
    );

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
}
