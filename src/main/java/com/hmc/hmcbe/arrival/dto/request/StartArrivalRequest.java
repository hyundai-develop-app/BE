package com.hmc.hmcbe.arrival.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** destinationParkingLotId는 목적지 건물 주차장이 확인된 경우에만 보낸다. */
public record StartArrivalRequest(
		@NotBlank @Size(max = 100) String destinationName,
		@NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
		@NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
		Long destinationParkingLotId,
		@NotNull Instant expectedArrivalAt
) {
}
