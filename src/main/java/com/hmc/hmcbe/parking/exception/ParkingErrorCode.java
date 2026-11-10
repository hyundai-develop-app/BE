package com.hmc.hmcbe.parking.exception;

import com.hmc.hmcbe.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** parking 도메인(주차 기록) 에러 코드. 접두사 PARKING. */
@Getter
@RequiredArgsConstructor
public enum ParkingErrorCode implements ErrorCode {

	INVALID_PARKING_RECORD(HttpStatus.BAD_REQUEST, "PARKING4001", "주차 기록 정보가 올바르지 않습니다"),
	INVALID_SPOT_MEMO(HttpStatus.BAD_REQUEST, "PARKING4002", "층·구역 메모가 올바르지 않습니다"),
	INVALID_EXIT_TIME(HttpStatus.BAD_REQUEST, "PARKING4003", "출차 시각이 올바르지 않습니다"),
	PARKING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "PARKING4041", "주차 기록을 찾을 수 없습니다"),
	ACTIVE_PARKING_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "PARKING4042", "진행 중인 주차 기록이 없습니다"),
	ALREADY_EXITED(HttpStatus.CONFLICT, "PARKING4091", "이미 출차한 주차 기록입니다");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;
}
