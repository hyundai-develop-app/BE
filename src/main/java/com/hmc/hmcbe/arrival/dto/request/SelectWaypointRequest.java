package com.hmc.hmcbe.arrival.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "경유 주차장 선택 요청")
public record SelectWaypointRequest(
		@Schema(description = "경유지로 넣을 주차장 ID. 보통 브리핑의 recommended.parkingLot.id. 목적지 주차장과 같으면 400 ARRIVAL4002",
				example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull Long parkingLotId) {
}
