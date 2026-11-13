package com.hmc.hmcbe.parkinglot.fixture;

import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import com.hmc.hmcbe.parkinglot.entity.ParkingFee;
import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class ParkingLotFixture {

	/** 여의도 공영주차장 근처 좌표. */
	public static final GeoPoint YEOUIDO = new GeoPoint(37.5259, 126.9284);

	private ParkingLotFixture() {
	}

	public static ParkingLot parkingLot(String name, GeoPoint location) {
		return parkingLot(name, location, null, null, false);
	}

	public static ParkingLot parkingLot(String name, GeoPoint location, String address,
			String feeDescription, boolean weekendOnly) {
		return ParkingLot.create(name, location, true, "건물 뒷편 골목", 120, new ParkingFee(10, 1000), 300,
				address, feeDescription, weekendOnly);
	}

	public static ParkingLot parkingLot() {
		return parkingLot("여의도 공영주차장", YEOUIDO);
	}

	public static ParkingLot parkingLotWithId(Long id) {
		ParkingLot lot = parkingLot();
		ReflectionTestUtils.setField(lot, "id", id);
		return lot;
	}

	public static ParkingLot parkingLotWithId(Long id, String address, String feeDescription, boolean weekendOnly) {
		ParkingLot lot = parkingLot("여의도 공영주차장", YEOUIDO, address, feeDescription, weekendOnly);
		ReflectionTestUtils.setField(lot, "id", id);
		return lot;
	}

	public static OccupancyForecast forecast(Long parkingLotId, Instant targetAt, int rate) {
		return OccupancyForecast.create(parkingLotId, targetAt, rate);
	}
}
