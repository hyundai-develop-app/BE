package com.hmc.hmcbe.arrival.controller.docs;

import com.hmc.hmcbe.arrival.dto.request.SelectWaypointRequest;
import com.hmc.hmcbe.arrival.dto.request.StartArrivalRequest;
import com.hmc.hmcbe.arrival.dto.response.ArrivalOutlookResponse;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;
import com.hmc.hmcbe.arrival.dto.response.ParkingBriefingResponse;
import com.hmc.hmcbe.global.docs.ApiErrorCodes;
import com.hmc.hmcbe.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Arrival", description = "도착 모드: 도착 시각 주차장 예측, 도착 10분 전 브리핑, 경유지 추가")
public interface ArrivalControllerDocs {

	@Operation(summary = "도착 모드 시작", description = "목적지를 설정하면 시작한다. 진행 중인 세션이 있으면 취소하고 새로 시작한다.")
	@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "시작됨. data는 새 세션(status=ACTIVE)")
	@ApiErrorCodes({"ARRIVAL4001", "PARKINGLOT4041"})
	ResponseEntity<ApiResponse<ArrivalSessionResponse>> start(Long userId, StartArrivalRequest request);

	@Operation(summary = "진행 중인 도착 모드 조회", description = "앱 재진입 시 진행 중인 세션을 복구할 때 쓴다. 없으면 404 ARRIVAL4042.")
	@ApiErrorCodes({"ARRIVAL4042"})
	ResponseEntity<ApiResponse<ArrivalSessionResponse>> getActiveSession(Long userId);

	@Operation(summary = "주행 중 대기 화면",
			description = "목적지 주차장의 도착 시각 예측, 도착 전후 1시간 시간대별 만차율, 더 여유 있는 주변 주차장 수.")
	@ApiErrorCodes({"ARRIVAL4041"})
	ResponseEntity<ApiResponse<ArrivalOutlookResponse>> getOutlook(Long userId,
			@Parameter(description = "도착 모드 세션 ID", example = "10") Long sessionId);

	@Operation(summary = "도착 10분 전 주차 브리핑", description = "목적지 주차장과 더 여유 있는 추천 주차장. 목적지 반경 500m 안에서 고른다.")
	@ApiErrorCodes({"ARRIVAL4041"})
	ResponseEntity<ApiResponse<ParkingBriefingResponse>> getBriefing(Long userId,
			@Parameter(description = "도착 모드 세션 ID", example = "10") Long sessionId);

	@Operation(summary = "경유 주차장 선택", description = "브리핑에서 \"응\"이라고 답하면 추천 주차장을 경유지로 넣는다. 다시 보내면 경유지가 바뀐다.")
	@ApiErrorCodes({"ARRIVAL4002", "ARRIVAL4041", "PARKINGLOT4041", "ARRIVAL4091"})
	ResponseEntity<ApiResponse<ArrivalSessionResponse>> selectWaypoint(Long userId,
			@Parameter(description = "도착 모드 세션 ID", example = "10") Long sessionId,
			SelectWaypointRequest request);

	@Operation(summary = "도착 모드 완료", description = "성공하면 data는 null.")
	@ApiErrorCodes({"ARRIVAL4041", "ARRIVAL4091"})
	ResponseEntity<ApiResponse<Void>> complete(Long userId,
			@Parameter(description = "도착 모드 세션 ID", example = "10") Long sessionId);

	@Operation(summary = "도착 모드 취소", description = "성공하면 data는 null.")
	@ApiErrorCodes({"ARRIVAL4041", "ARRIVAL4091"})
	ResponseEntity<ApiResponse<Void>> cancel(Long userId,
			@Parameter(description = "도착 모드 세션 ID", example = "10") Long sessionId);
}
