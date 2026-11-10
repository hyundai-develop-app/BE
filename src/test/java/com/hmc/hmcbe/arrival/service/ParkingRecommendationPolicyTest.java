package com.hmc.hmcbe.arrival.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.hmc.hmcbe.arrival.service.ParkingRecommendationPolicy.Recommendation;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParkingRecommendationPolicyTest {

	private final ParkingRecommendationPolicy policy = new ParkingRecommendationPolicy();

	@Test
	@DisplayName("목적지 주차장보다 만차율이 낮은 주차장만 대안으로, 만차율·거리 순으로 고른다")
	void recommendLessCongested() {
		// given
		NearbyParkingLotResponse destination = candidate(1L, 0, 95, "CONGESTED");
		NearbyParkingLotResponse far = candidate(2L, 450, 40, "AVAILABLE");
		NearbyParkingLotResponse near = candidate(3L, 300, 40, "AVAILABLE");
		NearbyParkingLotResponse busier = candidate(4L, 100, 97, "CONGESTED");
		NearbyParkingLotResponse unknown = candidate(5L, 50, null, null);

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, unknown, busier, near, far));

		// then
		assertThat(result.destination()).isEqualTo(destination);
		assertThat(result.recommended()).isEqualTo(near);
		assertThat(result.alternatives()).containsExactly(near, far);
	}

	@Test
	@DisplayName("목적지 주차장 예측이 없으면 혼잡하지 않은 주차장을 대안으로 본다")
	void destinationWithoutForecast() {
		NearbyParkingLotResponse destination = candidate(1L, 0, null, null);
		NearbyParkingLotResponse normal = candidate(2L, 200, 80, "NORMAL");
		NearbyParkingLotResponse congested = candidate(3L, 100, 92, "CONGESTED");

		Recommendation result = policy.recommend(1L, List.of(destination, congested, normal));

		assertThat(result.alternatives()).containsExactly(normal);
	}

	@Test
	@DisplayName("목적지 주차장이 없으면 혼잡하지 않은 주변 주차장을 대안으로 본다")
	void noDestinationLot() {
		NearbyParkingLotResponse available = candidate(2L, 200, 30, "AVAILABLE");

		Recommendation result = policy.recommend(null, List.of(available));

		assertThat(result.destination()).isNull();
		assertThat(result.recommended()).isEqualTo(available);
	}

	@Test
	@DisplayName("더 나은 대안이 없으면 추천하지 않는다")
	void noBetterAlternative() {
		NearbyParkingLotResponse destination = candidate(1L, 0, 30, "AVAILABLE");
		NearbyParkingLotResponse busier = candidate(2L, 200, 60, "AVAILABLE");

		Recommendation result = policy.recommend(1L, List.of(destination, busier));

		assertThat(result.recommended()).isNull();
		assertThat(result.alternatives()).isEmpty();
	}

	private static NearbyParkingLotResponse candidate(Long id, int distance, Integer rate, String level) {
		ParkingLotResponse lot = new ParkingLotResponse(id, "주차장" + id, 37.5, 126.9, true, null, 120, 10, 1000, 100);
		OccupancyForecastResponse forecast = rate == null
				? null
				: new OccupancyForecastResponse(Instant.parse("2026-11-09T05:00:00Z"), rate, level);
		return new NearbyParkingLotResponse(lot, distance, forecast);
	}
}
