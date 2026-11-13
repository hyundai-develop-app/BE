package com.hmc.hmcbe.parkinglot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 기준 위치 주변 주차장. 기준 시각의 예측이 없으면 forecast는 null이다. */
@Schema(description = "기준 위치 주변 주차장과 기준 시각의 예측")
public record NearbyParkingLotResponse(
		@Schema(description = "주차장 정보")
		ParkingLotResponse parkingLot,
		@Schema(description = "기준 위치에서의 직선 거리(m, 반올림)", example = "320")
		int distanceMeters,
		@Schema(description = "기준 시각이 속한 30분 슬롯의 예측. 예측이 없으면 null", nullable = true)
		OccupancyForecastResponse forecast
) {
	public static NearbyParkingLotResponse of(ParkingLotResponse parkingLot, double distanceMeters,
			OccupancyForecastResponse forecast) {
		return new NearbyParkingLotResponse(parkingLot, (int) Math.round(distanceMeters), forecast);
	}
}
