package com.guideon.core.domain.place.entity;

import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.util.Map;

@Entity
@Table(
        name = "tb_place",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_place_id_site", columnNames = {"place_id", "site_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_source", nullable = false, length = 10)
    private ZoneSource zoneSource;

    @Column(nullable = false, length = 100)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "name_json", columnDefinition = "jsonb")
    private Map<String, String> nameJson;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    public Place(Site site, Zone zone, ZoneSource zoneSource, String name,
                 Map<String, String> nameJson, String category, Point location,
                 String description, String imageUrl, Boolean isActive) {
        this.site = site;
        this.zone = zone;
        this.zoneSource = zoneSource != null ? zoneSource : ZoneSource.AUTO;
        this.name = name;
        this.nameJson = nameJson;
        this.category = category;
        this.location = location;
        this.description = description;
        this.imageUrl = imageUrl;
        this.isActive = isActive != null ? isActive : true;
    }

    public void assignZone(Zone zone) {
        this.zone = zone;
    }
}
