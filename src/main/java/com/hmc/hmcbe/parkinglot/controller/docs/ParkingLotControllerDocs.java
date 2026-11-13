package com.hmc.hmcbe.parkinglot.controller.docs;

import com.hmc.hmcbe.global.docs.ApiErrorCodes;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "ParkingLot", description = "주차장 정보와 예측 만차율")
public interface ParkingLotControllerDocs {

	@Operation(summary = "주차장 조회")
	@ApiErrorCodes({"PARKINGLOT4041"})
	ResponseEntity<ApiResponse<ParkingLotResponse>> getParkingLot(
			@Parameter(description = "주차장 ID", example = "1") Long parkingLotId);

	@Operation(summary = "주변 주차장 조회",
			description = "반경 안의 주차장을 가까운 순(최대 10개)으로, 기준 시각이 속한 30분 슬롯의 예측 만차율과 함께 반환한다. 없으면 빈 배열.")
	@ApiErrorCodes({"PARKINGLOT4004", "COMMON4002"})
	ResponseEntity<ApiResponse<List<NearbyParkingLotResponse>>> findNearby(
			@Parameter(description = "기준 위도(-90 ~ 90)", example = "37.5453") double latitude,
			@Parameter(description = "기준 경도(-180 ~ 180)", example = "127.2236") double longitude,
			@Parameter(description = "예측 기준 시각(ISO-8601). (at - 30분, at] 구간의 가장 늦은 예측을 쓴다.",
					example = "2026-11-14T09:30:00Z") Instant at,
			@Parameter(description = "검색 반경(m, 1 ~ 5000). 기본 500", example = "500") int radius);

	@Operation(summary = "예측 만차율 조회", description = "from 이상 to 이하 구간의 예측을 시각 순으로 반환한다. 없으면 빈 배열.")
	@ApiErrorCodes({"PARKINGLOT4005", "PARKINGLOT4041"})
	ResponseEntity<ApiResponse<List<OccupancyForecastResponse>>> getForecasts(
			@Parameter(description = "주차장 ID", example = "1") Long parkingLotId,
			@Parameter(description = "조회 시작 시각(포함)", example = "2026-11-14T08:30:00Z") Instant from,
			@Parameter(description = "조회 끝 시각(포함). from보다 이르면 400 PARKINGLOT4005", example = "2026-11-14T10:30:00Z")
			Instant to);
}
