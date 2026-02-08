package com.guideon.core.domain.place.repository;

import com.guideon.core.domain.place.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByPlaceIdAndSite_SiteId(Long placeId, Long siteId);

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
