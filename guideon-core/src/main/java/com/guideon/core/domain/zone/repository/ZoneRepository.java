package com.guideon.core.domain.zone.repository;

import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.domain.zone.entity.ZoneType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, Long> {

    boolean existsBySite_SiteIdAndCode(Long siteId, String code);

    boolean existsBySite_SiteIdAndCodeAndZoneIdNot(Long siteId, String code, Long zoneId);

    Page<Zone> findBySite_SiteId(Long siteId, Pageable pageable);

    Page<Zone> findBySite_SiteIdAndZoneType(Long siteId, ZoneType zoneType, Pageable pageable);

    Page<Zone> findBySite_SiteIdAndParentZone_ZoneId(Long siteId, Long parentZoneId, Pageable pageable);

    Optional<Zone> findByZoneIdAndSite_SiteId(Long zoneId, Long siteId);

    /**
     * 특정 Zone의 자식 SUB 구역 목록 조회 (삭제 시 사용)
     */
    List<Zone> findByParentZone_ZoneId(Long parentZoneId);

    /**
     * SUB 폴리곤이 부모 INNER 영역 안에 포함되는지 검증
     */
    @Query(value = """
            SELECT ST_Contains(
                z.area_geometry,
                ST_SetSRID(ST_GeomFromGeoJSON(:geoJson), 4326)
            )
            FROM tb_zone z
            WHERE z.zone_id = :parentId
            """, nativeQuery = true)
    boolean isContainedByParent(@Param("parentId") Long parentId,
                                @Param("geoJson") String geoJson);

    /**
     * 동일 부모 아래 다른 SUB와 면적 겹침 여부 검증
     * ST_Intersects이면서 ST_Touches가 아닌 경우 = 실제 면적 겹침
     */
    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM tb_zone z
            WHERE z.parent_zone_id = :parentId
              AND ST_Intersects(z.area_geometry, ST_SetSRID(ST_GeomFromGeoJSON(:geoJson), 4326))
              AND NOT ST_Touches(z.area_geometry, ST_SetSRID(ST_GeomFromGeoJSON(:geoJson), 4326))
            """, nativeQuery = true)
    boolean hasOverlappingSiblings(@Param("parentId") Long parentId,
                                   @Param("geoJson") String geoJson);

    /**
     * 동일 부모 아래 다른 SUB와 면적 겹침 여부 검증 (자기 자신 제외, 수정 시 사용)
     */
    @Query(value = """
            SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END
            FROM tb_zone z
            WHERE z.parent_zone_id = :parentId
              AND z.zone_id != :excludeZoneId
              AND ST_Intersects(z.area_geometry, ST_SetSRID(ST_GeomFromGeoJSON(:geoJson), 4326))
              AND NOT ST_Touches(z.area_geometry, ST_SetSRID(ST_GeomFromGeoJSON(:geoJson), 4326))
            """, nativeQuery = true)
    boolean hasOverlappingSiblingsExcluding(@Param("parentId") Long parentId,
                                            @Param("geoJson") String geoJson,
                                            @Param("excludeZoneId") Long excludeZoneId);
}
