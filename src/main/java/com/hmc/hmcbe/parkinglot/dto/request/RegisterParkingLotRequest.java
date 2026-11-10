package com.hmc.hmcbe.parkinglot.dto.request;

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
		@Min(1) Integer totalSpaces
) {
}
