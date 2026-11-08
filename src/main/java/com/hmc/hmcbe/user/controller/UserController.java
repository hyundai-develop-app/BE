package com.hmc.hmcbe.user.controller;

import com.hmc.hmcbe.global.auth.CurrentUserId;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.user.controller.docs.UserControllerDocs;
import com.hmc.hmcbe.user.dto.request.UpdatePushTokenRequest;
import com.hmc.hmcbe.user.dto.response.UserResponse;
import com.hmc.hmcbe.user.service.UserReadService;
import com.hmc.hmcbe.user.service.UserWriteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me")
public class UserController implements UserControllerDocs {

	private final UserReadService userReadService;
	private final UserWriteService userWriteService;

	@Override
	@GetMapping
	public ResponseEntity<ApiResponse<UserResponse>> getMe(@CurrentUserId Long userId) {
		return ResponseEntity.ok(ApiResponse.ok(userReadService.getUser(userId)));
	}

	@Override
	@PutMapping("/push-token")
	public ResponseEntity<ApiResponse<Void>> updatePushToken(@CurrentUserId Long userId,
			@Valid @RequestBody UpdatePushTokenRequest request) {
		userWriteService.updatePushToken(userId, request.pushToken());
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
