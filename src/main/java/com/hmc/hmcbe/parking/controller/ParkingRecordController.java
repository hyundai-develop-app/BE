package com.hmc.hmcbe.parking.controller;

import com.hmc.hmcbe.global.auth.CurrentUserId;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.parking.controller.docs.ParkingRecordControllerDocs;
import com.hmc.hmcbe.parking.dto.request.ParkRequest;
import com.hmc.hmcbe.parking.dto.request.UpdateSpotMemoRequest;
import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;
import com.hmc.hmcbe.parking.dto.response.ParkingSummaryResponse;
import com.hmc.hmcbe.parking.service.ParkingRecordReadService;
import com.hmc.hmcbe.parking.service.ParkingRecordWriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/parking-records")
public class ParkingRecordController implements ParkingRecordControllerDocs {

	private final ParkingRecordReadService parkingRecordReadService;
	private final ParkingRecordWriteService parkingRecordWriteService;

	@Override
	@PostMapping
	public ResponseEntity<ApiResponse<ParkingRecordResponse>> park(@CurrentUserId Long userId,
			@Valid @RequestBody ParkRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok(parkingRecordWriteService.park(userId, request)));
	}

	@Override
	@GetMapping("/active")
	public ResponseEntity<ApiResponse<ParkingRecordResponse>> getActiveRecord(@CurrentUserId Long userId) {
		return ResponseEntity.ok(ApiResponse.ok(parkingRecordReadService.getActiveRecord(userId)));
	}

	@Override
	@GetMapping("/active/summary")
	public ResponseEntity<ApiResponse<ParkingSummaryResponse>> getSummary(@CurrentUserId Long userId) {
		return ResponseEntity.ok(ApiResponse.ok(parkingRecordReadService.getSummary(userId)));
	}

	@Override
	@PutMapping("/{recordId}/spot-memo")
	public ResponseEntity<ApiResponse<ParkingRecordResponse>> updateSpotMemo(@CurrentUserId Long userId,
			@PathVariable Long recordId, @Valid @RequestBody UpdateSpotMemoRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(
				parkingRecordWriteService.updateSpotMemo(userId, recordId, request.spotMemo())));
	}

	@Override
	@PostMapping("/{recordId}/exit")
	public ResponseEntity<ApiResponse<Void>> exit(@CurrentUserId Long userId, @PathVariable Long recordId) {
		parkingRecordWriteService.exit(userId, recordId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
