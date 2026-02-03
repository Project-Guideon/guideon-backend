package com.guideon.guideonbackend.global.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.valid.IsValidOp;

import java.util.*;

public class GeoJsonUtil {
    //PrecisionModel : 좌표 정밀도 모델(precision model)
    //4326 : 좌표계(위도/경도)
    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeoJsonUtil() {}

    /**
     * GeoJSON Map → JTS Geometry 변환
     */
    @SuppressWarnings("unchecked")
    public static Geometry toGeometry(Map<String, Object> geoJson) {
        //geoJson null 체크
        if (geoJson == null) {
            throw new IllegalArgumentException("GeoJSON 객체는 필수입니다");
        }

        // type 검증
        String type = (String) geoJson.get("type");
        if (!"Polygon".equals(type)) {
            throw new IllegalArgumentException("Polygon 타입만 지원합니다");
        }

        // coordinates 구조 검증
        Object rawCoords = geoJson.get("coordinates");
        if (!(rawCoords instanceof List<?> coordsList) || coordsList.isEmpty()) {
            throw new IllegalArgumentException("coordinates는 비어 있지 않은 배열이어야 합니다");
        }

        //List(ring) List(points) List(x,y)  ex) [x1, y1], [x2, y2], ... , [x1, y1]
        List<List<List<Number>>> coordinates = (List<List<List<Number>>>) rawCoords;

        LinearRing shell = createLinearRing(coordinates.get(0)); //첫 번째 링을 coordinates.get(0) 외곽선(shell)로 사용
        LinearRing[] holes = new LinearRing[coordinates.size() - 1]; //외곽선 안에 포함 안하는 부분(hole 구멍) 없으면 안써도 됨
        for (int i = 1; i < coordinates.size(); i++) {
            holes[i - 1] = createLinearRing(coordinates.get(i));
        }

        // IsValidOp으로 상세 유효성 체크(자기교차, 홀 관계 등)
        Polygon polygon = FACTORY.createPolygon(shell, holes);
        IsValidOp validOp = new IsValidOp(polygon);
        if (!validOp.isValid()) {
            throw new IllegalArgumentException(
                    "유효하지 않은 폴리곤: " + validOp.getValidationError().getMessage());
        }

        return polygon;
    }

    /**
     * JTS Geometry → GeoJSON Map 변환
     */
    public static Map<String, Object> toGeoJson(Geometry geometry) {
        if (!(geometry instanceof Polygon polygon)) {
            throw new IllegalArgumentException("Polygon 타입만 지원합니다");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", "Polygon");

        List<List<List<Double>>> coords = new ArrayList<>();
        coords.add(ringToCoords(polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            coords.add(ringToCoords(polygon.getInteriorRingN(i)));
        }
        result.put("coordinates", coords);

        return result;
    }

    /**
     * GeoJSON Map → JSON 문자열 (네이티브 쿼리 ST_GeomFromGeoJSON 파라미터용)
     */
    public static String toJsonString(Map<String, Object> geoJson) {
        try {
            return MAPPER.writeValueAsString(geoJson);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("GeoJSON 직렬화 실패", e);
        }
    }

    private static LinearRing createLinearRing(List<List<Number>> points) {
        // 최소 점 체크: 닫힌 링은 최소 4개 좌표 필요 (3개 꼭짓점 + 닫기) 나중에 수정해도 됨
        if (points == null || points.size() < 4) {
            throw new IllegalArgumentException("링은 최소 4개의 좌표가 필요합니다 (3개 꼭짓점 + 닫기)");
        }

        //각 좌표쌍 검증 및 유한값/범위 체크
        for (int i = 0; i < points.size(); i++) {
            List<Number> p = points.get(i);
            if (p == null || p.size() < 2 || p.get(0) == null || p.get(1) == null) {
                throw new IllegalArgumentException("좌표[" + i + "]는 [경도, 위도] 형식이어야 합니다");
            }
            double lng = p.get(0).doubleValue();
            double lat = p.get(1).doubleValue();
            if (!Double.isFinite(lng) || !Double.isFinite(lat)) {
                throw new IllegalArgumentException("좌표[" + i + "] 값이 유한한 숫자가 아닙니다");
            }
            if (lng < -180 || lng > 180 || lat < -90 || lat > 90) {
                throw new IllegalArgumentException(
                        "좌표[" + i + "] 범위 초과 (경도: -180~180, 위도: -90~90)");
            }
        }

        // 닫힌 링 체크: 첫 점과 마지막 점이 동일해야 함 (GeoJSON 스펙)
        List<Number> first = points.get(0);
        List<Number> last = points.get(points.size() - 1);
        if (first.get(0).doubleValue() != last.get(0).doubleValue()
                || first.get(1).doubleValue() != last.get(1).doubleValue()) {
            throw new IllegalArgumentException("링의 첫 좌표와 마지막 좌표가 동일해야 합니다 (닫힌 링)");
        }

        Coordinate[] coords = points.stream()
                .map(p -> new Coordinate(p.get(0).doubleValue(), p.get(1).doubleValue()))
                .toArray(Coordinate[]::new);
        return FACTORY.createLinearRing(coords);
    }

    private static List<List<Double>> ringToCoords(LineString ring) {
        return Arrays.stream(ring.getCoordinates())
                .map(c -> List.of(c.x, c.y))
                .toList();
    }
}
