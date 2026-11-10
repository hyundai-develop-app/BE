package com.hmc.hmcbe.user.exception;

import com.hmc.hmcbe.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** user 도메인 에러 코드. 접두사 USER. */
@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

	INVALID_AUTH_SUBJECT(HttpStatus.BAD_REQUEST, "USER4001", "인증 subject가 올바르지 않습니다"),
	INVALID_PUSH_TOKEN(HttpStatus.BAD_REQUEST, "USER4002", "푸시 토큰이 올바르지 않습니다"),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER4041", "사용자를 찾을 수 없습니다");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;
}
