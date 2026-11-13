package com.hmc.hmcbe.global.response;

import com.hmc.hmcbe.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "공통 응답. 성공이면 data, 실패면 error가 채워진다.")
public record ApiResponse<T>(
		@Schema(description = "성공 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
		boolean success,
		@Schema(description = "응답 데이터. 본문이 없는 성공과 실패에서는 null", nullable = true)
		T data,
		@Schema(description = "실패 정보. 성공이면 null", nullable = true)
		ErrorBody error
) {
	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data, null);
	}

	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null, null);
	}

	public static ApiResponse<Void> fail(ErrorCode code, String message) {
		return new ApiResponse<>(false, null, new ErrorBody(code.getCode(), message));
	}

	@Schema(description = "실패 정보")
	public record ErrorBody(
			@Schema(description = "에러 코드", example = "PARKING4041") String code,
			@Schema(description = "에러 메시지", example = "주차 기록을 찾을 수 없습니다") String message) {
	}
}
