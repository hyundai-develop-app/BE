package com.hmc.hmcbe.parking.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** P단 변속 시 차량이 보내는 GPS 위치. 주차장이 확인된 경우에만 parkingLotId를 보낸다. */
@Schema(description = "P단 주차 기록 요청")
public record ParkRequest(
		@Schema(description = "주차 위치 위도", example = "37.5453", minimum = "-90", maximum = "90",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
		@Schema(description = "주차 위치 경도", example = "127.2236", minimum = "-180", maximum = "180",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
		@Schema(description = "주차장 ID. 확인된 경우에만 보낸다. 없는 ID면 404 PARKINGLOT4041. 보내지 않으면 주차 위치 300m 안 가장 가까운 주차장으로 기록한다", example = "1",
				nullable = true)
		Long parkingLotId
) {
}
