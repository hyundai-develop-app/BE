package com.hmc.hmcbe.user.dto.response;

import com.hmc.hmcbe.user.entity.User;

public record UserResponse(Long id, boolean pushTokenRegistered) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getPushToken() != null);
	}
}
