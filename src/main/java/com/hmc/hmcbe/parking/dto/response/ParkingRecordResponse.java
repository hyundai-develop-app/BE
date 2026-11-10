package com.hmc.hmcbe.parking.dto.response;

import com.hmc.hmcbe.parking.entity.ParkingRecord;
import java.time.Instant;

public record ParkingRecordResponse(
		Long id,
		Long parkingLotId,
		double latitude,
		double longitude,
		Instant parkedAt,
		String spotMemo,
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
