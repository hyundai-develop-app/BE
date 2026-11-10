package com.hmc.hmcbe.parkinglot.controller;

import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.parkinglot.controller.docs.ParkingLotControllerDocs;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parking-lots")
public class ParkingLotController implements ParkingLotControllerDocs {

	private final ParkingLotReadService parkingLotReadService;

	@Override
	@GetMapping("/{parkingLotId}")
	public ResponseEntity<ApiResponse<ParkingLotResponse>> getParkingLot(@PathVariable Long parkingLotId) {
		return ResponseEntity.ok(ApiResponse.ok(parkingLotReadService.getParkingLot(parkingLotId)));
	}

	@Override
	@GetMapping("/nearby")
	public ResponseEntity<ApiResponse<List<NearbyParkingLotResponse>>> findNearby(
			@RequestParam double latitude,
			@RequestParam double longitude,
			@RequestParam Instant at,
			@RequestParam(defaultValue = "500") int radius) {
		return ResponseEntity.ok(ApiResponse.ok(
				parkingLotReadService.findNearby(new GeoPoint(latitude, longitude), radius, at)));
	}

	@Override
	@GetMapping("/{parkingLotId}/forecasts")
	public ResponseEntity<ApiResponse<List<OccupancyForecastResponse>>> getForecasts(
			@PathVariable Long parkingLotId,
			@RequestParam Instant from,
			@RequestParam Instant to) {
		return ResponseEntity.ok(ApiResponse.ok(parkingLotReadService.getForecasts(parkingLotId, from, to)));
	}
}
