package com.hmc.hmcbe.arrival.exception;

import com.hmc.hmcbe.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** arrival 도메인(도착 모드) 에러 코드. 접두사 ARRIVAL. */
@Getter
@RequiredArgsConstructor
public enum ArrivalErrorCode implements ErrorCode {

	INVALID_ARRIVAL_SESSION(HttpStatus.BAD_REQUEST, "ARRIVAL4001", "도착 모드 정보가 올바르지 않습니다"),
	INVALID_WAYPOINT(HttpStatus.BAD_REQUEST, "ARRIVAL4002", "경유 주차장이 올바르지 않습니다"),
	ARRIVAL_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "ARRIVAL4041", "도착 모드를 찾을 수 없습니다"),
	ACTIVE_ARRIVAL_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "ARRIVAL4042", "진행 중인 도착 모드가 없습니다"),
	ALREADY_ENDED(HttpStatus.CONFLICT, "ARRIVAL4091", "이미 끝난 도착 모드입니다");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;
}
