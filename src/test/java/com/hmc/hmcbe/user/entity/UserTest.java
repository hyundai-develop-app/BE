package com.hmc.hmcbe.user.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.user.exception.UserErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserTest {

	@Test
	@DisplayName("인증 subject로 사용자를 만들면 푸시 토큰 없이 생성된다")
	void create() {
		// when
		User user = User.create("issuer-sub-1");

		// then
		assertThat(user.getAuthSubject()).isEqualTo("issuer-sub-1");
		assertThat(user.getPushToken()).isNull();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  "})
	@DisplayName("인증 subject가 비어 있으면 사용자를 만들 수 없다")
	void createWithBlankSubject(String subject) {
		BusinessException ex = assertThrows(BusinessException.class, () -> User.create(subject));

		assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.INVALID_AUTH_SUBJECT);
	}

	@Test
	@DisplayName("인증 subject가 255자를 넘으면 사용자를 만들 수 없다")
	void createWithTooLongSubject() {
		BusinessException ex = assertThrows(BusinessException.class, () -> User.create("a".repeat(256)));

		assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.INVALID_AUTH_SUBJECT);
	}

	@Test
	@DisplayName("푸시 토큰을 등록하면 기존 토큰을 교체한다")
	void updatePushToken() {
		// given
		User user = User.create("sub");
		user.updatePushToken("old-token");

		// when
		user.updatePushToken("new-token");

		// then
		assertThat(user.getPushToken()).isEqualTo("new-token");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {"  "})
	@DisplayName("빈 푸시 토큰은 등록할 수 없다")
	void updateBlankPushToken(String token) {
		User user = User.create("sub");

		BusinessException ex = assertThrows(BusinessException.class, () -> user.updatePushToken(token));

		assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.INVALID_PUSH_TOKEN);
	}

	@Test
	@DisplayName("512자를 넘는 푸시 토큰은 등록할 수 없다")
	void updateTooLongPushToken() {
		User user = User.create("sub");

		BusinessException ex = assertThrows(BusinessException.class, () -> user.updatePushToken("a".repeat(513)));

		assertThat(ex.getErrorCode()).isEqualTo(UserErrorCode.INVALID_PUSH_TOKEN);
	}
}
