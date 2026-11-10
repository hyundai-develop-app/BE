package com.hmc.hmcbe.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

	@Test
	@DisplayName("메시지 없이 생성하면 ErrorCode의 기본 메시지를 사용한다")
	void defaultMessage() {
		// when
		BusinessException ex = new BusinessException(CommonErrorCode.INVALID_REQUEST);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST);
		assertThat(ex.getMessage()).isEqualTo(CommonErrorCode.INVALID_REQUEST.getDefaultMessage());
	}

	@Test
	@DisplayName("원인 예외와 함께 생성하면 cause가 보존된다")
	void keepsCause() {
		// given
		IllegalStateException cause = new IllegalStateException();

		// when
		BusinessException ex = new BusinessException(CommonErrorCode.INTERNAL_ERROR, "실패", cause);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_ERROR);
		assertThat(ex.getCause()).isSameAs(cause);
	}
}
