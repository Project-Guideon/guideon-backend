package com.guideon.core.domain.place.repository;

import com.guideon.core.domain.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
     * 좌표가 포함된 Zone ID 조회 (AUTO zone 할당용)
     * 가장 작은 레벨(SUB 다음 INNER 다음 outer)의 Zone을 반환
     * 어떤 Zone에도 포함되지 않으면 null 반환 (OUTER)
     */
    @Query(value = """
            SELECT z.zone_id
            FROM tb_zone z
            WHERE z.site_id = :siteId
              AND ST_Contains(z.area_geometry, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326))
            ORDER BY z.level DESC
            LIMIT 1
            """, nativeQuery = true)
    Long findZoneIdByCoordinates(
            @Param("siteId") Long siteId,
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude
    );
}
