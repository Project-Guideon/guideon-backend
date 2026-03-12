package com.guideon.core.domain.dailyinfo.entity;

import com.guideon.core.domain.place.entity.Place;
import com.guideon.core.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "tb_daily_info",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_daily", columnNames = {"place_id", "target_date", "info_type"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyInfo extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", referencedColumnName = "place_id",
            insertable = false, updatable = false)
    private Place place;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "info_type", nullable = false, length = 50)
    private String infoType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public DailyInfo(Long siteId, Long placeId, Place place, LocalDate targetDate, String infoType, String content) {
        this.siteId = siteId;
        this.placeId = placeId;
        this.place = place;
        this.targetDate = targetDate;
        this.infoType = infoType;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }
}