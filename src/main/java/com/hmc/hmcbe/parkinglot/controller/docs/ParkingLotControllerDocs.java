package com.hmc.hmcbe.parkinglot.controller.docs;

import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "ParkingLot", description = "주차장 정보와 예측 만차율")
public interface ParkingLotControllerDocs {

	@Operation(summary = "주차장 조회")
	ResponseEntity<ApiResponse<ParkingLotResponse>> getParkingLot(Long parkingLotId);

	@Operation(summary = "주변 주차장 조회",
			description = "반경 안의 주차장을 가까운 순(최대 10개)으로, 기준 시각이 속한 30분 슬롯의 예측 만차율과 함께 반환한다.")
	ResponseEntity<ApiResponse<List<NearbyParkingLotResponse>>> findNearby(double latitude, double longitude,
			Instant at, int radius);

	@Operation(summary = "예측 만차율 조회", description = "from 이상 to 이하 구간의 예측을 시각 순으로 반환한다.")
	ResponseEntity<ApiResponse<List<OccupancyForecastResponse>>> getForecasts(Long parkingLotId, Instant from,
			Instant to);
}
