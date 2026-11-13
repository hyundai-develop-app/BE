package com.hmc.hmcbe.parking.dto.response;

import com.hmc.hmcbe.parking.entity.ParkingRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "주차 기록")
public record ParkingRecordResponse(
		@Schema(description = "주차 기록 ID", example = "100")
		Long id,
		@Schema(description = "주차장 ID. 모르면 null", example = "1", nullable = true)
		Long parkingLotId,
		@Schema(description = "주차 위치 위도", example = "37.5453")
		double latitude,
		@Schema(description = "주차 위치 경도", example = "127.2236")
		double longitude,
		@Schema(description = "주차 시각(서버 기록 시각)", example = "2026-11-14T09:32:10Z")
		Instant parkedAt,
		@Schema(description = "층·구역 메모. 저장 전이면 null", example = "지하 2층 C구역", nullable = true)
		String spotMemo,
		@Schema(description = "출차 시각. 주차 중이면 null", nullable = true)
		Instant exitedAt
) {
	public static ParkingRecordResponse from(ParkingRecord record) {
		return new ParkingRecordResponse(
				record.getId(),
				record.getParkingLotId(),
				record.getLocation().latitude(),
				record.getLocation().longitude(),
				record.getParkedAt(),
				record.getSpotMemo(),
				record.getExitedAt());
	}
}
