package com.hmc.hmcbe.global.geo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.locationtech.jts.geom.Point;

class GeoPointTest {

	@Test
	@DisplayName("JTS Point로 바꾸면 경도가 x, 위도가 y이고 SRID는 4326이다")
	void toPoint() {
		// given
		GeoPoint geoPoint = new GeoPoint(37.5259, 126.9284);

		// when
		Point point = geoPoint.toPoint();

		// then
		assertThat(point.getX()).isEqualTo(126.9284);
		assertThat(point.getY()).isEqualTo(37.5259);
		assertThat(point.getSRID()).isEqualTo(4326);
		assertThat(GeoPoint.from(point)).isEqualTo(geoPoint);
	}

	@ParameterizedTest
	@CsvSource({"90.0001, 0", "-90.0001, 0", "0, 180.0001", "0, -180.0001", "NaN, 0"})
	@DisplayName("위도·경도 범위를 벗어나면 만들 수 없다")
	void outOfRange(double latitude, double longitude) {
		BusinessException ex = assertThrows(BusinessException.class, () -> new GeoPoint(latitude, longitude));

		assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_COORDINATE);
	}

	@ParameterizedTest
	@CsvSource({"90, 180", "-90, -180", "0, 0"})
	@DisplayName("경계값 좌표는 만들 수 있다")
	void boundary(double latitude, double longitude) {
		assertThat(new GeoPoint(latitude, longitude).latitude()).isEqualTo(latitude);
	}
}
