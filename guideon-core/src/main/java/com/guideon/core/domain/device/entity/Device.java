package com.guideon.core.domain.device.entity;

import com.guideon.core.domain.place.entity.ZoneSource;
import com.guideon.core.domain.site.entity.Site;
import com.guideon.core.domain.zone.entity.Zone;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;

import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "tb_device")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device extends BaseEntity implements Persistable<String> {

    @Id
    @Column(name = "device_id", length = 50)
    private String deviceId;

    @Column(name = "auth_token_hash", nullable = false, length = 64)
    private String authTokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Enumerated(EnumType.STRING)
    @Column(name = "zone_source", nullable = false, length = 10)
    private ZoneSource zoneSource;

    @Column(name = "location_name", nullable = false, length = 100)
    private String locationName;

    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "last_ping")
    private LocalDateTime lastPing;

    @Column(name = "last_auth_at")
    private LocalDateTime lastAuthAt;

    @Builder
    public Device(String deviceId, String authTokenHash, Site site, Zone zone,
                  ZoneSource zoneSource, String locationName, Point location, Boolean isActive) {
        this.deviceId = deviceId;
        this.authTokenHash = authTokenHash;
        this.site = site;
        this.zone = zone;
        this.zoneSource = zoneSource != null ? zoneSource : ZoneSource.AUTO;
        this.locationName = locationName;
        this.location = location;
        this.isActive = isActive != null ? isActive : true;
    }

    public void update(String locationName, Point location, Boolean isActive) {
        if (locationName != null) this.locationName = locationName;
        if (location != null) this.location = location;
        if (isActive != null) this.isActive = isActive;
    }

    public void changeZone(Zone zone, ZoneSource zoneSource) {
        this.zone = zone;
        this.zoneSource = zoneSource;
    }

    public void rotateToken(String newAuthTokenHash) {
        this.authTokenHash = newAuthTokenHash;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateLastPing() {
        this.lastPing = LocalDateTime.now();
    }

    public void updateLastAuthAt() {
        this.lastAuthAt = LocalDateTime.now();
    }

    @Override
    public String getId() {
        return deviceId;
    }

    @Override
    public boolean isNew() {
        return getCreatedAt() == null;
    }
}
