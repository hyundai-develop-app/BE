package com.hmc.hmcbe.parkinglot.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class OccupancyForecastTest {

	private static final Instant TARGET_AT = Instant.parse("2026-11-09T05:30:00Z");

	@Test
	@DisplayName("예측 만차율을 만들면 주차장·대상 시각·만차율이 저장된다")
	void create() {
		OccupancyForecast forecast = OccupancyForecast.create(1L, TARGET_AT, 95);

		assertThat(forecast.getParkingLotId()).isEqualTo(1L);
		assertThat(forecast.getTargetAt()).isEqualTo(TARGET_AT);
		assertThat(forecast.getOccupancyRate()).isEqualTo(95);
		assertThat(forecast.congestionLevel()).isEqualTo(CongestionLevel.CONGESTED);
	}

	@ParameterizedTest
	@ValueSource(ints = {-1, 101})
	@DisplayName("만차율이 0~100을 벗어나면 만들 수 없다")
	void invalidRate(int rate) {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> OccupancyForecast.create(1L, TARGET_AT, rate));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("주차장이나 대상 시각이 없으면 만들 수 없다")
	void missingRequired() {
		assertThat(assertThrows(BusinessException.class, () -> OccupancyForecast.create(null, TARGET_AT, 50))
				.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
		assertThat(assertThrows(BusinessException.class, () -> OccupancyForecast.create(1L, null, 50))
				.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("예측을 다시 계산하면 만차율을 교체한다")
	void updateRate() {
		OccupancyForecast forecast = OccupancyForecast.create(1L, TARGET_AT, 50);

		forecast.updateOccupancyRate(80);

		assertThat(forecast.getOccupancyRate()).isEqualTo(80);
	}

	@Test
	@DisplayName("범위를 벗어난 만차율로 교체할 수 없다")
	void updateInvalidRate() {
		OccupancyForecast forecast = OccupancyForecast.create(1L, TARGET_AT, 50);

		assertThrows(BusinessException.class, () -> forecast.updateOccupancyRate(101));
		assertThat(forecast.getOccupancyRate()).isEqualTo(50);
	}

	@ParameterizedTest
	@CsvSource({"0, AVAILABLE", "69, AVAILABLE", "70, NORMAL", "89, NORMAL", "90, CONGESTED", "100, CONGESTED"})
	@DisplayName("만차율 70% 미만은 여유, 90% 미만은 보통, 그 이상은 혼잡이다")
	void congestionLevel(int rate, CongestionLevel expected) {
		assertThat(CongestionLevel.of(rate)).isEqualTo(expected);
	}
}
