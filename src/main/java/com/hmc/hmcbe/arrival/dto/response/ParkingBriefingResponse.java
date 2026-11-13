package com.hmc.hmcbe.arrival.dto.response;

import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * ② 도착 10분 전 주차 브리핑: 목적지 주차장 카드, 추천 주차장 카드, 대안 목록(추천 포함, 만차율·거리 순).
 * 목적지 주차장·추천이 없으면 null이다.
 */
@Schema(description = "도착 10분 전 주차 브리핑")
public record ParkingBriefingResponse(
		@Schema(description = "목적지 주차장 카드. 목적지 주차장을 모르거나 반경 밖이면 null", nullable = true)
		NearbyParkingLotResponse destinationParkingLot,
		@Schema(description = "추천 주차장 카드(alternatives의 첫 번째). 대안이 없으면 null", nullable = true)
		NearbyParkingLotResponse recommended,
		@Schema(description = """
				목적지보다 여유 있는 대안 주차장(recommended 포함). 만차율 낮은 순, 같으면 가까운 순. 없으면 빈 배열.
				목적지 예측이 있으면 그보다 만차율이 낮은 곳, 없으면 CONGESTED가 아닌 곳이 대안이다. 예측이 없는 주차장은 빠진다.""")
		List<NearbyParkingLotResponse> alternatives
) {
}
