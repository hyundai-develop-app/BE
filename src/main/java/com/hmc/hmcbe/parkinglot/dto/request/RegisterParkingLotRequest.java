package com.hmc.hmcbe.parkinglot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** feeUnitMinutes와 feeUnitPrice는 함께 주거나 함께 생략한다. */
public record RegisterParkingLotRequest(
		@NotBlank @Size(max = 100) String name,
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
		@NotNull Boolean indoor,
		@Size(max = 255) String entranceGuide,
		@NotNull @Min(0) Integer freeMinutes,
		@Min(1) Integer feeUnitMinutes,
		@Min(0) Integer feeUnitPrice,
		@Min(1) Integer totalSpaces,
		@Schema(description = "주소. 모르면 null", example = "서울 강남구 영동대로 513 지하", nullable = true)
		@Size(max = 255) String address,
		@Schema(description = "요금 안내 문구(앱이 그대로 읽어 준다). 모르면 null", example = "10분 800원", nullable = true)
		@Size(max = 255) String feeDescription,
		@Schema(description = "토·일에만 운영(공휴일은 판단하지 않음). 평일 주변 검색에서는 빠진다", example = "false")
		Boolean weekendOnly
) {
}
