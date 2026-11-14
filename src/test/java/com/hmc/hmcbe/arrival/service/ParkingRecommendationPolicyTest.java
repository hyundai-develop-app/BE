package com.hmc.hmcbe.arrival.service;

import static com.hmc.hmcbe.parkinglot.fixture.NearbyParkingLotFixture.withoutForecast;
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
		ParkingLotResponse lot = new ParkingLotResponse(id, "주차장" + id, 37.5, 126.9, true, null, 120, 10, 1000, 100, null, null, false);
		OccupancyForecastResponse forecast = rate == null
				? null
				: new OccupancyForecastResponse(Instant.parse("2026-11-09T05:00:00Z"), rate, level);
		return new NearbyParkingLotResponse(lot, distance, forecast);
	}

	@Test
	@DisplayName("예측이 없으면 목적지보다 큰 첫 주차장을 추천하고 나머지 대안은 거리순으로 반환한다")
	void fallbackPrefersFirstLargerLot() {
		// given
		NearbyParkingLotResponse destination = withoutForecast(1L, 0, 100);
		NearbyParkingLotResponse unknown = withoutForecast(2L, 50, null);
		NearbyParkingLotResponse equal = withoutForecast(3L, 100, 100);
		NearbyParkingLotResponse smaller = withoutForecast(4L, 150, 80);
		NearbyParkingLotResponse larger = withoutForecast(5L, 600, 120);
		NearbyParkingLotResponse largest = withoutForecast(6L, 650, 300);

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, unknown, equal, smaller, larger, largest));

		// then
		assertThat(result.destination()).isEqualTo(destination);
		assertThat(result.recommended()).isEqualTo(larger);
		assertThat(result.alternatives()).containsExactly(larger, unknown, equal, smaller, largest);
	}

	@Test
	@DisplayName("목적지 크기를 모르면 더 큰 주차장이 있어도 가장 가까운 다른 주차장을 추천한다")
	void fallbackWithUnknownDestinationSize() {
		// given
		NearbyParkingLotResponse destination = withoutForecast(1L, 0, null);
		NearbyParkingLotResponse nearest = withoutForecast(2L, 100, null);
		NearbyParkingLotResponse large = withoutForecast(3L, 200, 300);

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, nearest, large));

		// then
		assertThat(result.recommended()).isEqualTo(nearest);
		assertThat(result.alternatives()).containsExactly(nearest, large);
	}

	@Test
	@DisplayName("목적지보다 큰 주차장이 없으면 크기를 모르는 가장 가까운 다른 주차장도 추천한다")
	void fallbackWithoutLargerLot() {
		// given
		NearbyParkingLotResponse destination = withoutForecast(1L, 0, 100);
		NearbyParkingLotResponse nearest = withoutForecast(2L, 100, null);
		NearbyParkingLotResponse equal = withoutForecast(3L, 200, 100);
		NearbyParkingLotResponse smaller = withoutForecast(4L, 300, 80);

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, nearest, equal, smaller));

		// then
		assertThat(result.recommended()).isEqualTo(nearest);
		assertThat(result.alternatives()).containsExactly(nearest, equal, smaller);
	}

	@Test
	@DisplayName("목적지 예측이 있고 더 나은 예측이 없으면 예측 없는 큰 주차장도 추천하지 않는다")
	void destinationForecastPreventsFallback() {
		// given
		NearbyParkingLotResponse destination = candidate(1L, 0, 30, "AVAILABLE");
		NearbyParkingLotResponse unknown = withoutForecast(2L, 50, 500);
		NearbyParkingLotResponse equal = candidate(3L, 100, 30, "AVAILABLE");
		NearbyParkingLotResponse busier = candidate(4L, 200, 60, "NORMAL");

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, unknown, equal, busier));

		// then
		assertThat(result.destination()).isEqualTo(destination);
		assertThat(result.recommended()).isNull();
		assertThat(result.alternatives()).isEmpty();
	}

	@Test
	@DisplayName("예측 규칙으로 대안이 있으면 예측 없는 큰 주차장보다 만차율·거리순 대안을 우선한다")
	void forecastRecommendationsTakePriorityOverFallback() {
		// given
		NearbyParkingLotResponse destination = withoutForecast(1L, 0, 100);
		NearbyParkingLotResponse large = withoutForecast(2L, 50, 500);
		NearbyParkingLotResponse normal = candidate(3L, 100, 80, "NORMAL");
		NearbyParkingLotResponse near = candidate(4L, 200, 30, "AVAILABLE");
		NearbyParkingLotResponse far = candidate(5L, 300, 30, "AVAILABLE");

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, large, normal, near, far));

		// then
		assertThat(result.recommended()).isEqualTo(near);
		assertThat(result.alternatives()).containsExactly(near, far, normal);
	}

	@Test
	@DisplayName("혼잡 예측만 있고 예측 없는 후보가 있으면 혼잡 후보를 대체 풀에서 제외한다")
	void fallbackExcludesCongestedForecasts() {
		// given
		NearbyParkingLotResponse destination = withoutForecast(1L, 0, 100);
		NearbyParkingLotResponse congested = candidate(2L, 50, 95, "CONGESTED");
		NearbyParkingLotResponse unknown = withoutForecast(3L, 100, 80);

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, congested, unknown));

		// then
		assertThat(result.recommended()).isEqualTo(unknown);
		assertThat(result.alternatives()).containsExactly(unknown);
	}

	@Test
	@DisplayName("목적지 외에 혼잡 예측 후보만 있으면 대체 풀이 비어 추천하지 않는다")
	void noFallbackPool() {
		// given
		NearbyParkingLotResponse destination = withoutForecast(1L, 0, 100);
		NearbyParkingLotResponse congested = candidate(2L, 50, 95, "CONGESTED");

		// when
		Recommendation result = policy.recommend(1L, List.of(destination, congested));

		// then
		assertThat(result.recommended()).isNull();
		assertThat(result.alternatives()).isEmpty();
	}

	@Test
	@DisplayName("목적지 ID가 없거나 후보에서 찾지 못하면 예측 없는 가장 가까운 후보를 추천한다")
	void fallbackWithoutDestination() {
		// given
		NearbyParkingLotResponse nearest = withoutForecast(2L, 100, 80);
		NearbyParkingLotResponse large = withoutForecast(3L, 200, 500);

		// when
		Recommendation noId = policy.recommend(null, List.of(nearest, large));
		Recommendation missingId = policy.recommend(99L, List.of(nearest, large));

		// then
		assertThat(noId.destination()).isNull();
		assertThat(noId.recommended()).isEqualTo(nearest);
		assertThat(noId.alternatives()).containsExactly(nearest, large);
		assertThat(missingId).isEqualTo(noId);
	}

	@Test
	@DisplayName("후보가 없거나 목적지만 있으면 추천과 대안이 없다")
	void noOtherCandidate() {
		// given
		NearbyParkingLotResponse destination = withoutForecast(1L, 0, 100);

		// when
		Recommendation empty = policy.recommend(null, List.of());
		Recommendation onlyDestination = policy.recommend(1L, List.of(destination));

		// then
		assertThat(empty.destination()).isNull();
		assertThat(empty.recommended()).isNull();
		assertThat(empty.alternatives()).isEmpty();
		assertThat(onlyDestination.destination()).isEqualTo(destination);
		assertThat(onlyDestination.recommended()).isNull();
		assertThat(onlyDestination.alternatives()).isEmpty();
	}

	@Test
	@DisplayName("목적지가 가장 가까운 후보가 아니어도 지정한 ID의 크기를 기준으로 대체 추천한다")
	void fallbackUsesDestinationId() {
		// given
		NearbyParkingLotResponse nearest = withoutForecast(2L, 50, 80);
		NearbyParkingLotResponse larger = withoutForecast(3L, 100, 150);
		NearbyParkingLotResponse destination = withoutForecast(1L, 600, 100);

		// when
		Recommendation result = policy.recommend(1L, List.of(nearest, larger, destination));

		// then
		assertThat(result.destination()).isEqualTo(destination);
		assertThat(result.recommended()).isEqualTo(larger);
		assertThat(result.alternatives()).containsExactly(larger, nearest);
	}
}
