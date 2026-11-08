package com.hmc.hmcbe.arrival.service;

import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 도착 시각 기준으로 목적지 주차장보다 여유 있는 주변 주차장을 고르는 규칙(7.4 ~Policy). 리포지토리·외부 호출 없이 순수 로직만 둔다.
 * <ul>
 * <li>목적지 주차장 예측이 있으면: 그보다 만차율이 낮은 주차장이 대안이다.</li>
 * <li>목적지 주차장이 없거나 예측이 없으면: 혼잡(CONGESTED)이 아닌 주차장이 대안이다.</li>
 * <li>예측이 없는 주차장은 대안으로 보지 않는다. 대안은 만차율, 거리 순으로 정렬하고 첫 번째를 추천한다.</li>
 * </ul>
 */
@Component
public class ParkingRecommendationPolicy {

	private static final String CONGESTED = "CONGESTED";

	public Recommendation recommend(Long destinationParkingLotId, List<NearbyParkingLotResponse> candidates) {
		NearbyParkingLotResponse destination = candidates.stream()
				.filter(c -> c.parkingLot().id().equals(destinationParkingLotId))
				.findFirst()
				.orElse(null);

		List<NearbyParkingLotResponse> alternatives = candidates.stream()
				.filter(c -> c != destination && c.forecast() != null)
				.filter(c -> isBetterThan(c, destination))
				.sorted(Comparator.comparingInt((NearbyParkingLotResponse c) -> c.forecast().occupancyRate())
						.thenComparingInt(NearbyParkingLotResponse::distanceMeters))
				.toList();

		return new Recommendation(destination, alternatives.isEmpty() ? null : alternatives.get(0), alternatives);
	}

	private boolean isBetterThan(NearbyParkingLotResponse candidate, NearbyParkingLotResponse destination) {
		if (destination == null || destination.forecast() == null) {
			return !CONGESTED.equals(candidate.forecast().congestionLevel());
		}
		return candidate.forecast().occupancyRate() < destination.forecast().occupancyRate();
	}

	/** destination·recommended는 없으면 null이다. alternatives는 recommended를 포함한다. */
	public record Recommendation(
			NearbyParkingLotResponse destination,
			NearbyParkingLotResponse recommended,
			List<NearbyParkingLotResponse> alternatives
	) {
	}
}
