package com.hmc.hmcbe.global.geo;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** WGS84(SRID 4326) 좌표 값 객체. DB의 geometry(Point, 4326) 컬럼과 JTS Point로 변환한다. */
public record GeoPoint(double latitude, double longitude) {

	public static final int SRID = 4326;

	private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), SRID);

	public GeoPoint {
		if (!(latitude >= -90 && latitude <= 90)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "위도는 -90 ~ 90 사이여야 합니다");
		}
		if (!(longitude >= -180 && longitude <= 180)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "경도는 -180 ~ 180 사이여야 합니다");
		}
	}

	public static GeoPoint from(Point point) {
		return new GeoPoint(point.getY(), point.getX());
	}

	public Point toPoint() {
		return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
	}
}
