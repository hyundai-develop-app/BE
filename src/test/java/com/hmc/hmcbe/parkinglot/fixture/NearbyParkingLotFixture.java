package com.hmc.hmcbe.parkinglot.fixture;

import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import java.time.Instant;

public final class NearbyParkingLotFixture {

	private NearbyParkingLotFixture() {
	}

	public static NearbyParkingLotResponse withoutForecast(Long id, int distanceMeters, Integer totalSpaces) {
		return candidate(id, distanceMeters, totalSpaces, null, null);
	}

	public static NearbyParkingLotResponse candidate(Long id, int distanceMeters, Integer totalSpaces,
			Integer rate, String level) {
		ParkingLotResponse lot = new ParkingLotResponse(id, "주차장" + id, 37.5, 126.9, true, null,
				120, 10, 1000, totalSpaces, null, null, false);
		OccupancyForecastResponse forecast = rate == null ? null
				: new OccupancyForecastResponse(Instant.parse("2026-11-09T05:18:00Z"), rate, level);
		return new NearbyParkingLotResponse(lot, distanceMeters, forecast);
	}
}
