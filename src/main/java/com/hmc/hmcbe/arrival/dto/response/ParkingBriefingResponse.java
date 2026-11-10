package com.hmc.hmcbe.arrival.dto.response;

import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import java.util.List;

/**
 * ② 도착 10분 전 주차 브리핑: 목적지 주차장 카드, 추천 주차장 카드, 대안 목록(추천 포함, 만차율·거리 순).
 * 목적지 주차장·추천이 없으면 null이다.
 */
public record ParkingBriefingResponse(
		NearbyParkingLotResponse destinationParkingLot,
		NearbyParkingLotResponse recommended,
		List<NearbyParkingLotResponse> alternatives
) {
}
