package com.hmc.hmcbe.place.controller;

import com.hmc.hmcbe.global.auth.CurrentUserId;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.place.controller.docs.SavedPlaceControllerDocs;
import com.hmc.hmcbe.place.dto.request.CreateSavedPlaceRequest;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.place.service.SavedPlaceReadService;
import com.hmc.hmcbe.place.service.SavedPlaceWriteService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/saved-places")
public class SavedPlaceController implements SavedPlaceControllerDocs {

	private final SavedPlaceReadService savedPlaceReadService;
	private final SavedPlaceWriteService savedPlaceWriteService;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponse<List<SavedPlaceResponse>>> getSavedPlaces(@CurrentUserId Long userId) {
		return ResponseEntity.ok(ApiResponse.ok(savedPlaceReadService.getSavedPlaces(userId)));
	}

	@Override
	@PostMapping
	public ResponseEntity<ApiResponse<SavedPlaceResponse>> create(@CurrentUserId Long userId,
			@Valid @RequestBody CreateSavedPlaceRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.ok(savedPlaceWriteService.create(userId, request)));
	}

	@Override
	@DeleteMapping("/{savedPlaceId}")
	public ResponseEntity<ApiResponse<Void>> delete(@CurrentUserId Long userId, @PathVariable Long savedPlaceId) {
		savedPlaceWriteService.delete(userId, savedPlaceId);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
