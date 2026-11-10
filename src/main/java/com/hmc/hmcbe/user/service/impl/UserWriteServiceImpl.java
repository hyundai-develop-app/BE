package com.hmc.hmcbe.user.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.user.entity.User;
import com.hmc.hmcbe.user.exception.UserErrorCode;
import com.hmc.hmcbe.user.repository.UserRepository;
import com.hmc.hmcbe.user.service.UserWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserWriteServiceImpl implements UserWriteService {

	private final UserRepository userRepository;

	@Override
	public Long resolveUserId(String authSubject) {
		return userRepository.findByAuthSubject(authSubject)
				.orElseGet(() -> userRepository.save(User.create(authSubject)))
				.getId();
	}

	@Override
	public void updatePushToken(Long userId, String pushToken) {
		findUserOrThrow(userId).updatePushToken(pushToken);
	}

	private User findUserOrThrow(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
	}
}
