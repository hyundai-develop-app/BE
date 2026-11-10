package com.hmc.hmcbe.parking.fixture;

import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parking.entity.ParkingRecord;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class ParkingRecordFixture {

	public static final GeoPoint LOCATION = new GeoPoint(37.5262, 126.9290);
	/** 14:20 KST. */
	public static final Instant PARKED_AT = Instant.parse("2026-11-09T05:20:00Z");

	private ParkingRecordFixture() {
	}

	public static ParkingRecord activeRecord(Long userId) {
		return ParkingRecord.record(userId, null, LOCATION, PARKED_AT);
	}

	public static ParkingRecord activeRecord(Long userId, Long parkingLotId) {
		return ParkingRecord.record(userId, parkingLotId, LOCATION, PARKED_AT);
	}

	public static ParkingRecord withId(ParkingRecord record, Long id) {
		ReflectionTestUtils.setField(record, "id", id);
		return record;
	}
}
