package com.hmc.hmcbe.arrival.fixture;

import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.global.geo.GeoPoint;
import java.time.Instant;
import org.springframework.test.util.ReflectionTestUtils;

public final class ArrivalSessionFixture {

	/** 더현대 서울 근처 좌표. */
	public static final GeoPoint DESTINATION = new GeoPoint(37.5259, 126.9284);
	public static final Instant ETA = Instant.parse("2026-11-09T05:18:00Z");

	private ArrivalSessionFixture() {
	}

	public static ArrivalSession activeSession(Long userId) {
		return ArrivalSession.start(userId, "더현대 서울", DESTINATION, null, ETA);
	}

	public static ArrivalSession activeSession(Long userId, Long destinationParkingLotId) {
		return ArrivalSession.start(userId, "더현대 서울", DESTINATION, destinationParkingLotId, ETA);
	}

	public static ArrivalSession withId(ArrivalSession session, Long id) {
		ReflectionTestUtils.setField(session, "id", id);
		return session;
	}
}
