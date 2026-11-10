package com.hmc.hmcbe.parking.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** P단 변속 시 차량이 보내는 GPS 위치. 주차장이 확인된 경우에만 parkingLotId를 보낸다. */
public record ParkRequest(
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
		Long parkingLotId
) {
}
