package com.hmc.hmcbe.arrival.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** destinationParkingLotId는 목적지 건물 주차장이 확인된 경우에만 보낸다. */
@Schema(description = "도착 모드 시작 요청")
public record StartArrivalRequest(
		@Schema(description = "목적지 이름(최대 100자)", example = "스타필드 하남", maxLength = 100,
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank @Size(max = 100) String destinationName,
		@Schema(description = "목적지 위도", example = "37.5453", minimum = "-90", maximum = "90",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
		@Schema(description = "목적지 경도", example = "127.2236", minimum = "-180", maximum = "180",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
		@Schema(description = "목적지 건물 주차장 ID. 확인된 경우에만 보낸다. 없는 ID면 404 PARKINGLOT4041", example = "1",
				nullable = true)
		Long destinationParkingLotId,
		@Schema(description = "예상 도착 시각(ISO-8601)", example = "2026-11-14T09:30:00Z",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull Instant expectedArrivalAt
) {
}
