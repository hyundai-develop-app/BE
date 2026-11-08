package com.hmc.hmcbe.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

	@Test
	@DisplayName("메시지 없이 생성하면 ErrorCode의 기본 메시지를 사용한다")
	void defaultMessage() {
		// when
		BusinessException ex = new BusinessException(ErrorCode.CONFLICT);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
		assertThat(ex.getMessage()).isEqualTo(ErrorCode.CONFLICT.getDefaultMessage());
	}

	@Test
	@DisplayName("원인 예외와 함께 생성하면 cause가 보존된다")
	void keepsCause() {
		// given
		IllegalStateException cause = new IllegalStateException();

		// when
		BusinessException ex = new BusinessException(ErrorCode.INTERNAL_ERROR, "실패", cause);

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
		assertThat(ex.getCause()).isSameAs(cause);
	}
}
