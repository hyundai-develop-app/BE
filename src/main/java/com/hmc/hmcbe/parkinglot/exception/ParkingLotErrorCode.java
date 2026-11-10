package com.hmc.hmcbe.parkinglot.exception;

import com.hmc.hmcbe.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** parkinglot 도메인 에러 코드. 접두사 PARKINGLOT. */
@Getter
@RequiredArgsConstructor
public enum ParkingLotErrorCode implements ErrorCode {

	INVALID_PARKING_LOT(HttpStatus.BAD_REQUEST, "PARKINGLOT4001", "주차장 정보가 올바르지 않습니다"),
	INVALID_FEE(HttpStatus.BAD_REQUEST, "PARKINGLOT4002", "주차 요금 정보가 올바르지 않습니다"),
	INVALID_FORECAST(HttpStatus.BAD_REQUEST, "PARKINGLOT4003", "만차 예측 정보가 올바르지 않습니다"),
	INVALID_SEARCH_RADIUS(HttpStatus.BAD_REQUEST, "PARKINGLOT4004", "검색 반경이 허용 범위를 벗어났습니다"),
	INVALID_FORECAST_PERIOD(HttpStatus.BAD_REQUEST, "PARKINGLOT4005", "예측 조회 기간이 올바르지 않습니다"),
	PARKING_LOT_NOT_FOUND(HttpStatus.NOT_FOUND, "PARKINGLOT4041", "주차장을 찾을 수 없습니다");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;
}
