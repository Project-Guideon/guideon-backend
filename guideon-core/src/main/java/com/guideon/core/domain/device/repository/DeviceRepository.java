package com.guideon.core.domain.device.repository;

import com.guideon.core.domain.device.entity.Device;
import com.guideon.core.domain.place.entity.ZoneSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, String> {

    Optional<Device> findByDeviceIdAndSite_SiteId(String deviceId, Long siteId);

    Page<Device> findBySite_SiteId(Long siteId, Pageable pageable);

    boolean existsByDeviceId(String deviceId);

    Optional<Device> findByAuthTokenHashAndIsActiveTrue(String authTokenHash);

    /**
     * 좌표가 포함된 Zone ID 조회 (AUTO zone 할당용)
     * 가장 깊은 레벨(SUB > INNER)의 Zone을 반환, 없으면 null (OUTER)
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
     * site 내 AUTO zone 할당된 Device 목록 조회 (재계산 대상)
     */
    List<Device> findBySite_SiteIdAndZoneSource(Long siteId, ZoneSource zoneSource);
}
