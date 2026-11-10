package com.hmc.hmcbe.user.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.user.dto.response.UserResponse;
import com.hmc.hmcbe.user.exception.UserErrorCode;
import com.hmc.hmcbe.user.repository.UserRepository;
import com.hmc.hmcbe.user.service.UserReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReadServiceImpl implements UserReadService {

	private final UserRepository userRepository;

	@Override
	public UserResponse getUser(Long userId) {
		return userRepository.findById(userId)
				.map(UserResponse::from)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}
}
