package com.guideon.core.domain.zone.entity;

import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Geometry;

@Entity
@Table(
        name = "tb_zone",
        //유니크 설정
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_zone_code", columnNames = {"site_id", "code"}),
                @UniqueConstraint(name = "uk_zone_id_site", columnNames = {"zone_id", "site_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Zone extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Long zoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_type", nullable = false, length = 10)
    private ZoneType zoneType;

    /**
     * DB Generated Column: zone_type 기반 자동 계산 (INNER=1, SUB=2)
     * DB에서 자동 생성되므로 읽기 전용
     */
    @Column(name = "level", insertable = false, updatable = false)
    private Short level;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_zone_id")
    private Zone parentZone;

    @Column(name = "area_geometry", nullable = false, columnDefinition = "geometry(Polygon, 4326)")
    private Geometry areaGeometry;

    @Builder
    public Zone(Site site, String name, String code, ZoneType zoneType, Zone parentZone, Geometry areaGeometry) {
        this.site = site;
        this.name = name;
        this.code = code;
        this.zoneType = zoneType;
        this.parentZone = parentZone;
        this.areaGeometry = areaGeometry;
    }

    /**
     * 구역 정보 수정 (null이 아닌 필드만 반영)
     * zone_type과 parent_zone_id는 구조적 제약으로 변경 불가
     */
    public void update(String name, String code, Geometry areaGeometry) {
        if (name != null) this.name = name;
        if (code != null) this.code = code;
        if (areaGeometry != null) this.areaGeometry = areaGeometry;
    }
}
