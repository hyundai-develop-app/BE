package com.hmc.hmcbe.parkinglot.dto.response;

/** 기준 위치 주변 주차장. 기준 시각의 예측이 없으면 forecast는 null이다. */
public record NearbyParkingLotResponse(
		ParkingLotResponse parkingLot,
		int distanceMeters,
		OccupancyForecastResponse forecast
) {

	public static NearbyParkingLotResponse of(ParkingLotResponse parkingLot, double distanceMeters,
			OccupancyForecastResponse forecast) {
		return new NearbyParkingLotResponse(parkingLot, (int) Math.round(distanceMeters), forecast);
	}
}
