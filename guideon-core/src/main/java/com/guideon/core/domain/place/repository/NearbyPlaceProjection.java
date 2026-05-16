package com.guideon.core.domain.place.repository;

/**
 * Zone 우선순위 + ST_Distance 공간 검색 결과 프로젝션.
 * 네이티브 쿼리 결과를 매핑하기 위한 인터페이스.
 */
public interface NearbyPlaceProjection {
    Long getPlaceId();
    String getName();
    String getCategory();
    String getDescription();
    String getImageUrl();
    Double getLatitude();
    Double getLongitude();
    Double getDistanceM();
    Integer getZonePriority();
}
