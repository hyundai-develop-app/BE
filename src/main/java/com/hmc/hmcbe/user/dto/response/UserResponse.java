package com.hmc.hmcbe.user.dto.response;

import com.hmc.hmcbe.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내 정보")
public record UserResponse(
		@Schema(description = "사용자 ID", example = "42")
		Long id,
		@Schema(description = "폰 푸시 토큰 등록 여부", example = "true")
		boolean pushTokenRegistered) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getPushToken() != null);
	}
}
