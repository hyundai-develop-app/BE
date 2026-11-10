package com.hmc.hmcbe.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.user.dto.response.UserResponse;
import com.hmc.hmcbe.user.entity.User;
import com.hmc.hmcbe.user.exception.UserErrorCode;
import com.hmc.hmcbe.user.fixture.UserFixture;
import com.hmc.hmcbe.user.repository.UserRepository;
import com.hmc.hmcbe.user.service.impl.UserReadServiceImpl;
import com.hmc.hmcbe.user.service.impl.UserWriteServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserWriteServiceImpl userWriteService;

	@InjectMocks
	private UserReadServiceImpl userReadService;

	@Test
	@DisplayName("이미 있는 subject면 기존 사용자 ID를 반환하고 새로 만들지 않는다")
	void resolveExistingUser() {
		// given
		given(userRepository.findByAuthSubject(UserFixture.AUTH_SUBJECT))
				.willReturn(Optional.of(UserFixture.userWithId(1L)));

		// when
		Long userId = userWriteService.resolveUserId(UserFixture.AUTH_SUBJECT);

		// then
		assertThat(userId).isEqualTo(1L);
		verify(userRepository, never()).save(any());
	}

	@Test
	@DisplayName("처음 보는 subject면 사용자를 만들고 ID를 반환한다")
	void resolveNewUser() {
		// given
		given(userRepository.findByAuthSubject("new-sub")).willReturn(Optional.empty());
		given(userRepository.save(any(User.class))).willReturn(UserFixture.userWithId(2L));

		// when
		Long userId = userWriteService.resolveUserId("new-sub");

		// then
		assertThat(userId).isEqualTo(2L);
	}

	@Test
	@DisplayName("푸시 토큰을 등록하면 사용자에 저장된다")
	void updatePushToken() {
		// given
		User user = UserFixture.userWithId(1L);
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		// when
		userWriteService.updatePushToken(1L, "token");

		// then
		assertThat(user.getPushToken()).isEqualTo("token");
	}

	@Test
	@DisplayName("없는 사용자의 푸시 토큰을 등록하면 NOT_FOUND")
	void updatePushTokenOfUnknownUser() {
		given(userRepository.findById(9L)).willReturn(Optional.empty());

		BusinessException ex = assertThrows(BusinessException.class,
				() -> userWriteService.updatePushToken(9L, "token"));

		assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
	}

	@Test
	@DisplayName("사용자를 조회하면 푸시 토큰 등록 여부를 함께 반환한다")
	void getUser() {
		// given
		User user = UserFixture.userWithId(1L);
		user.updatePushToken("token");
		given(userRepository.findById(1L)).willReturn(Optional.of(user));

		// when
		UserResponse response = userReadService.getUser(1L);

		// then
		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.pushTokenRegistered()).isTrue();
	}

	@Test
	@DisplayName("없는 사용자를 조회하면 NOT_FOUND")
	void getUnknownUser() {
		given(userRepository.findById(9L)).willReturn(Optional.empty());

		BusinessException ex = assertThrows(BusinessException.class, () -> userReadService.getUser(9L));

		assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.USER_NOT_FOUND);
	}
}
