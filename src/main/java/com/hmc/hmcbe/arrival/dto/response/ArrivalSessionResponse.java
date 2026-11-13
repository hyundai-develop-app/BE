package com.hmc.hmcbe.arrival.dto.response;

import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "도착 모드 세션")
public record ArrivalSessionResponse(
		@Schema(description = "세션 ID", example = "10")
		Long id,
		@Schema(description = "목적지 이름", example = "스타필드 하남")
		String destinationName,
		@Schema(description = "목적지 위도", example = "37.5453")
		double latitude,
		@Schema(description = "목적지 경도", example = "127.2236")
		double longitude,
		@Schema(description = "목적지 건물 주차장 ID. 모르면 null", example = "1", nullable = true)
		Long destinationParkingLotId,
		@Schema(description = "선택한 경유 주차장 ID. 선택 전이면 null", example = "2", nullable = true)
		Long waypointParkingLotId,
		@Schema(description = "예상 도착 시각", example = "2026-11-14T09:30:00Z")
		Instant expectedArrivalAt,
		@Schema(description = "상태. ACTIVE(진행 중) / COMPLETED(완료) / CANCELLED(취소)", example = "ACTIVE")
		ArrivalStatus status
) {
	public static ArrivalSessionResponse from(ArrivalSession session) {
		return new ArrivalSessionResponse(
				session.getId(),
				session.getDestinationName(),
				session.getDestinationLocation().latitude(),
				session.getDestinationLocation().longitude(),
				session.getDestinationParkingLotId(),
				session.getWaypointParkingLotId(),
				session.getExpectedArrivalAt(),
				session.getStatus());
	}
}
