package com.hmc.hmcbe.place.exception;

import com.hmc.hmcbe.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** place 도메인 에러 코드. 접두사 PLACE. */
@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {

	INVALID_SAVED_PLACE(HttpStatus.BAD_REQUEST, "PLACE4001", "장소 정보가 올바르지 않습니다"),
	SAVED_PLACE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLACE4041", "장소를 찾을 수 없습니다"),
	DUPLICATE_PLACE_NAME(HttpStatus.CONFLICT, "PLACE4091", "같은 이름의 장소가 이미 있습니다");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;
}
