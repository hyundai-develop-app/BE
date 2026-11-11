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
				.orElseGet(() -> {
					// 엔티티 불변식으로 검증한다. 네이티브 INSERT의 생성 시각은 SQL에서 설정한다.
					String subject = User.create(authSubject).getAuthSubject();
					// READ COMMITTED에서 다음 SELECT가 새 스냅샷으로 충돌 상대의 커밋을 읽는다.
					userRepository.insertIfAbsent(subject);
					return userRepository.findByAuthSubject(subject)
							.orElseThrow(() -> new IllegalStateException("사용자 생성 후 재조회에 실패했습니다"));
				})
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
