package com.hmc.hmcbe.place.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "장소 저장 요청")
public record CreateSavedPlaceRequest(
		@Schema(description = "장소 이름(최대 50자). 사용자별로 중복될 수 없다.", example = "집", maxLength = 50,
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 50) String name,
		@Schema(description = "위도", example = "37.4979", minimum = "-90", maximum = "90",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
		@Schema(description = "경도", example = "127.0276", minimum = "-180", maximum = "180",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude
) {
}
