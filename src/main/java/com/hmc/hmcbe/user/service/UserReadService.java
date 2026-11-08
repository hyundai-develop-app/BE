package com.hmc.hmcbe.user.service;

import com.hmc.hmcbe.user.dto.response.UserResponse;

public interface UserReadService {

	UserResponse getUser(Long userId);
}
