package com.hmc.hmcbe.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** 특정 도메인에 속하지 않는 공통 에러 코드. */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "COMMON4001", "잘못된 요청입니다"),
	INVALID_COORDINATE(HttpStatus.BAD_REQUEST, "COMMON4002", "좌표가 올바르지 않습니다"),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON4011", "인증이 필요합니다"),
	FORBIDDEN(HttpStatus.FORBIDDEN, "COMMON4031", "접근 권한이 없습니다"),
	NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON4041", "요청한 경로를 찾을 수 없습니다"),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON4051", "지원하지 않는 HTTP 메서드입니다"),
	CONFLICT(HttpStatus.CONFLICT, "COMMON4091", "요청이 현재 데이터 상태와 충돌합니다"),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "COMMON4151", "지원하지 않는 Content-Type입니다"),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON5001", "서버 내부 오류가 발생했습니다");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;
}
