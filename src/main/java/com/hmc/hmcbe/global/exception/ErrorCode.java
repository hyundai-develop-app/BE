package com.hmc.hmcbe.global.exception;

import org.springframework.http.HttpStatus;

/**
 * API 실패 응답의 에러 코드 계약.
 *
 * <p>코드 형식은 {@code {DOMAIN}{HTTP 상태 3자리}{순번 1자리}}이다. 예: {@code USER4041} = user 도메인, 404, 1번.
 * DOMAIN은 도메인 패키지 이름을 대문자로 쓴 것이고, global은 {@code COMMON}을 쓴다. 형식은 ErrorCodeConventionTest가 검증한다.
 */
public interface ErrorCode {

	HttpStatus getStatus();

	String getCode();

	String getDefaultMessage();
}
