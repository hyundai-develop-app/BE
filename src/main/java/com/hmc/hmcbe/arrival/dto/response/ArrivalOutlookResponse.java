package com.hmc.hmcbe.arrival.dto.response;

import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * ① 주행 중 대기 화면: 목적지 주차장의 도착 시각 예측, 도착 전후 시간대별 만차율, 더 여유 있는 주변 주차장 수.
 * 목적지 주차장이 없으면 destinationParkingLot은 null, destinationForecasts는 빈 목록이다.
 */
@Schema(description = "주행 중 대기 화면 데이터")
public record ArrivalOutlookResponse(
		@Schema(description = "도착 모드 세션")
		ArrivalSessionResponse session,
		@Schema(description = "목적지 주차장과 도착 시각 예측. 목적지 주차장을 모르거나 반경 밖이면 null", nullable = true)
		NearbyParkingLotResponse destinationParkingLot,
		@Schema(description = "목적지 주차장의 도착 전후 1시간 예측(시각 순, 막대그래프용). 목적지 주차장이 없으면 빈 배열")
		List<OccupancyForecastResponse> destinationForecasts,
		@Schema(description = "목적지보다 여유 있는 주변 주차장 수(반경 500m)", example = "2")
		int betterAlternativeCount
) {
}
