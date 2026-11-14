package com.hmc.hmcbe.arrival.controller;

import com.hmc.hmcbe.arrival.controller.docs.ArrivalControllerDocs;
import com.hmc.hmcbe.arrival.dto.request.SelectWaypointRequest;
import com.hmc.hmcbe.arrival.dto.request.StartArrivalRequest;
import com.hmc.hmcbe.arrival.dto.response.ArrivalOutlookResponse;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;
import com.hmc.hmcbe.arrival.dto.response.ParkingBriefingResponse;
import com.hmc.hmcbe.arrival.service.ArrivalReadService;
import com.hmc.hmcbe.arrival.service.ArrivalWriteService;
import com.hmc.hmcbe.global.auth.CurrentUserId;
import com.hmc.hmcbe.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/arrivals")
public class ArrivalController implements ArrivalControllerDocs {

	private final ArrivalReadService arrivalReadService;
	private final ArrivalWriteService arrivalWriteService;

	@Override
	@PostMapping
	public ResponseEntity<ApiResponse<ArrivalSessionResponse>> start(@CurrentUserId Long userId,
			@Valid @RequestBody StartArrivalRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok(arrivalWriteService.start(userId, request)));
	}

	@Override
	@GetMapping("/active")
	public ResponseEntity<ApiResponse<ArrivalSessionResponse>> getActiveSession(@CurrentUserId Long userId) {
		return ResponseEntity.ok(ApiResponse.ok(arrivalReadService.getActiveSession(userId)));
	}

	@Override
	@GetMapping("/{sessionId}/outlook")
	public ResponseEntity<ApiResponse<ArrivalOutlookResponse>> getOutlook(@CurrentUserId Long userId,
			@PathVariable Long sessionId) {
		return ResponseEntity.ok(ApiResponse.ok(arrivalReadService.getOutlook(userId, sessionId)));
	}

	@Override
	@GetMapping("/{sessionId}/briefing")
	public ResponseEntity<ApiResponse<ParkingBriefingResponse>> getBriefing(@CurrentUserId Long userId,
			@PathVariable Long sessionId) {
		return ResponseEntity.ok(ApiResponse.ok(arrivalReadService.getBriefing(userId, sessionId)));
	}

	@Override
	@PutMapping("/{sessionId}/waypoint")
	public ResponseEntity<ApiResponse<ArrivalSessionResponse>> selectWaypoint(@CurrentUserId Long userId,
			@PathVariable Long sessionId, @Valid @RequestBody SelectWaypointRequest request) {
		return ResponseEntity.ok(ApiResponse.ok(
				arrivalWriteService.selectWaypoint(userId, sessionId, request.parkingLotId())));
	}

	@Override
	@DeleteMapping("/{sessionId}/waypoint")
	public ResponseEntity<ApiResponse<ArrivalSessionResponse>> clearWaypoint(@CurrentUserId Long userId,
			@PathVariable Long sessionId) {
		return ResponseEntity.ok(ApiResponse.ok(arrivalWriteService.clearWaypoint(userId, sessionId)));
	}

	@Override
	@PostMapping("/{sessionId}/complete")
	public ResponseEntity<ApiResponse<Void>> complete(@CurrentUserId Long userId, @PathVariable Long sessionId) {
		arrivalWriteService.complete(userId, sessionId);
		return ResponseEntity.ok(ApiResponse.ok());
	}

	@Override
	@PostMapping("/{sessionId}/cancel")
	public ResponseEntity<ApiResponse<Void>> cancel(@CurrentUserId Long userId, @PathVariable Long sessionId) {
		arrivalWriteService.cancel(userId, sessionId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
