package com.guideon.guideonbackend.domain.zone.entity;

import com.guideon.guideonbackend.domain.site.entity.Site;
import com.guideon.guideonbackend.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;
import org.locationtech.jts.geom.Geometry;

@Entity
@Table(
        name = "tb_zone",
        //우니크 설정
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
     * SQL 마이그레이션으로 관리 (ddl-auto로 생성/변경 불가)
     * @Column(name = "level", insertable = false, updatable = false)
     * 첫 생성 후 위에껄로 변경(왜 인식이 안되는지 모르겠는데, 얘가 못알아먹어서 오류 띄움)
     */
    @Generated(event = EventType.INSERT)
    @Column(
            nullable = false,
            insertable = false,
            updatable = false,
            columnDefinition = "SMALLINT GENERATED ALWAYS AS (CASE zone_type WHEN 'INNER' THEN 1 WHEN 'SUB' THEN 2 END) STORED"
    )
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
}