package com.hmc.hmcbe.parking.controller.docs;

import com.hmc.hmcbe.global.docs.ApiErrorCodes;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.parking.dto.request.ParkRequest;
import com.hmc.hmcbe.parking.dto.request.UpdateSpotMemoRequest;
import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;
import com.hmc.hmcbe.parking.dto.response.ParkingSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "ParkingRecord", description = "P단 주차 위치 기록과 다음 시동 주차 요약")
public interface ParkingRecordControllerDocs {

	@Operation(summary = "P단 주차 기록",
			description = "기어 P 변속 시 GPS 위치를 저장한다. 주차 시각은 서버 현재 시각이며, 출차하지 않은 이전 기록은 출차 처리한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "기록됨. data는 새 주차 기록")
	@ApiErrorCodes({"PARKING4001", "PARKINGLOT4041"})
	ResponseEntity<ApiResponse<ParkingRecordResponse>> park(Long userId, ParkRequest request);

	@Operation(summary = "진행 중인 주차 기록 조회", description = "출차하지 않은 기록. 없으면 404 PARKING4042.")
	@ApiErrorCodes({"PARKING4042"})
	ResponseEntity<ApiResponse<ParkingRecordResponse>> getActiveRecord(Long userId);

	@Operation(summary = "다음 시동 주차 요약", description = "경과 시간, 무료 시간, 남은 무료 시간, 빠른 목적지. 진행 중인 기록이 없으면 404 PARKING4042.")
	@ApiErrorCodes({"PARKING4042"})
	ResponseEntity<ApiResponse<ParkingSummaryResponse>> getSummary(Long userId);

	@Operation(summary = "층·구역 저장", description = "음성으로 들은 층·구역을 저장한다. 다시 보내면 정정된다. 출차한 기록이면 409.")
	@ApiErrorCodes({"PARKING4002", "PARKING4041", "PARKING4091"})
	ResponseEntity<ApiResponse<ParkingRecordResponse>> updateSpotMemo(Long userId,
			@Parameter(description = "주차 기록 ID", example = "100") Long recordId,
			UpdateSpotMemoRequest request);

	@Operation(summary = "출차", description = "출차 시각은 서버 현재 시각. 성공하면 data는 null.")
	@ApiErrorCodes({"PARKING4003", "PARKING4041", "PARKING4091"})
	ResponseEntity<ApiResponse<Void>> exit(Long userId,
			@Parameter(description = "주차 기록 ID", example = "100") Long recordId);
}
