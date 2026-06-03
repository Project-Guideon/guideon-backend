package com.guideon.core.domain.site.entity;

import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_site")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Site extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "site_id")
    private Long siteId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Admin 지도 페이지 초기 중심점 위도 (표시용, nullable) */
    @Column(name = "latitude")
    private Double latitude;

    /** Admin 지도 페이지 초기 중심점 경도 (표시용, nullable) */
    @Column(name = "longitude")
    private Double longitude;

    /** 카카오맵 줌 레벨(1~14, nullable) */
    @Column(name = "map_level")
    private Integer mapLevel;

    @Builder
    public Site(String name, Double latitude, Double longitude, Integer mapLevel) {
        this.name = name;
        this.isActive = true;
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapLevel = mapLevel;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public void updateName(String name) {
        this.name = name;
    }

    /** 지도 중심 좌표/줌 레벨 갱신 */
    public void updateMapLocation(Double latitude, Double longitude, Integer mapLevel) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.mapLevel = mapLevel;
    }
}
