package com.hmc.hmcbe.arrival.dto.response;

import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import java.util.List;

/**
 * ① 주행 중 대기 화면: 목적지 주차장의 도착 시각 예측, 도착 전후 시간대별 만차율, 더 여유 있는 주변 주차장 수.
 * 목적지 주차장이 없으면 destinationParkingLot은 null, destinationForecasts는 빈 목록이다.
 */
public record ArrivalOutlookResponse(
		ArrivalSessionResponse session,
		NearbyParkingLotResponse destinationParkingLot,
		List<OccupancyForecastResponse> destinationForecasts,
		int betterAlternativeCount
) {
}
