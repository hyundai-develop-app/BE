package com.hmc.hmcbe.arrival.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.arrival.exception.ArrivalErrorCode;
import com.hmc.hmcbe.arrival.fixture.ArrivalSessionFixture;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.geo.GeoPoint;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ArrivalSessionTest {

	private static final GeoPoint DESTINATION = ArrivalSessionFixture.DESTINATION;
	private static final Instant ETA = ArrivalSessionFixture.ETA;
	private static final Instant NOW = Instant.parse("2026-11-09T05:30:00Z");

	@Test
	@DisplayName("도착 모드를 시작하면 ACTIVE 상태로 목적지와 예상 도착 시각을 가진다")
	void start() {
		ArrivalSession session = ArrivalSession.start(1L, "더현대 서울", DESTINATION, 7L, ETA);

		assertThat(session.getUserId()).isEqualTo(1L);
		assertThat(session.getDestinationName()).isEqualTo("더현대 서울");
		assertThat(session.getDestinationLocation()).isEqualTo(DESTINATION);
		assertThat(session.getDestinationParkingLotId()).isEqualTo(7L);
		assertThat(session.getExpectedArrivalAt()).isEqualTo(ETA);
		assertThat(session.getStatus()).isEqualTo(ArrivalStatus.ACTIVE);
		assertThat(session.getWaypointParkingLotId()).isNull();
		assertThat(session.getEndedAt()).isNull();
	}

	@Test
	@DisplayName("목적지 주차장은 없어도 시작할 수 있다")
	void startWithoutDestinationLot() {
		ArrivalSession session = ArrivalSession.start(1L, "더현대 서울", DESTINATION, null, ETA);

		assertThat(session.getDestinationParkingLotId()).isNull();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" "})
	@DisplayName("목적지 이름이 비어 있으면 시작할 수 없다")
	void blankDestinationName(String name) {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ArrivalSession.start(1L, name, DESTINATION, null, ETA));

		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.INVALID_ARRIVAL_SESSION);
	}

	@Test
	@DisplayName("사용자·목적지 위치·예상 도착 시각이 없으면 시작할 수 없다")
	void missingRequired() {
		assertThrows(BusinessException.class, () -> ArrivalSession.start(null, "목적지", DESTINATION, null, ETA));
		assertThrows(BusinessException.class, () -> ArrivalSession.start(1L, "목적지", null, null, ETA));
		assertThrows(BusinessException.class, () -> ArrivalSession.start(1L, "목적지", DESTINATION, null, null));
	}

	@Test
	@DisplayName("진행 중인 세션에 경유 주차장을 넣으면 교체된다")
	void selectWaypoint() {
		ArrivalSession session = ArrivalSessionFixture.activeSession(1L);

		session.selectWaypoint(3L);
		session.selectWaypoint(4L);

		assertThat(session.getWaypointParkingLotId()).isEqualTo(4L);
	}

	@Test
	@DisplayName("경유 주차장 ID가 없으면 INVALID_REQUEST")
	void selectNullWaypoint() {
		ArrivalSession session = ArrivalSessionFixture.activeSession(1L);

		BusinessException ex = assertThrows(BusinessException.class, () -> session.selectWaypoint(null));

		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.INVALID_WAYPOINT);
	}

	@Test
	@DisplayName("목적지 주차장을 경유지로 넣을 수 없다")
	void selectDestinationLotAsWaypoint() {
		ArrivalSession session = ArrivalSession.start(1L, "더현대 서울", DESTINATION, 7L, ETA);

		BusinessException ex = assertThrows(BusinessException.class, () -> session.selectWaypoint(7L));

		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.INVALID_WAYPOINT);
	}

	@Test
	@DisplayName("완료하면 COMPLETED 상태가 되고 종료 시각이 기록된다")
	void complete() {
		ArrivalSession session = ArrivalSessionFixture.activeSession(1L);

		session.complete(NOW);

		assertThat(session.getStatus()).isEqualTo(ArrivalStatus.COMPLETED);
		assertThat(session.getEndedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("취소하면 CANCELLED 상태가 되고 종료 시각이 기록된다")
	void cancel() {
		ArrivalSession session = ArrivalSessionFixture.activeSession(1L);

		session.cancel(NOW);

		assertThat(session.getStatus()).isEqualTo(ArrivalStatus.CANCELLED);
		assertThat(session.getEndedAt()).isEqualTo(NOW);
	}

	@Test
	@DisplayName("끝난 세션은 다시 완료·취소하거나 경유지를 바꿀 수 없다")
	void endedSessionIsImmutable() {
		ArrivalSession session = ArrivalSessionFixture.activeSession(1L);
		session.cancel(NOW);

		assertThat(assertThrows(BusinessException.class, () -> session.complete(NOW)).getErrorCode())
				.isEqualTo(ArrivalErrorCode.ALREADY_ENDED);
		assertThat(assertThrows(BusinessException.class, () -> session.cancel(NOW)).getErrorCode())
				.isEqualTo(ArrivalErrorCode.ALREADY_ENDED);
		assertThat(assertThrows(BusinessException.class, () -> session.selectWaypoint(3L)).getErrorCode())
				.isEqualTo(ArrivalErrorCode.ALREADY_ENDED);
		assertThat(session.getStatus()).isEqualTo(ArrivalStatus.CANCELLED);
	}

	@Test
	@DisplayName("본인 세션인지 확인한다")
	void isOwnedBy() {
		ArrivalSession session = ArrivalSessionFixture.activeSession(1L);

		assertThat(session.isOwnedBy(1L)).isTrue();
		assertThat(session.isOwnedBy(2L)).isFalse();
	}
}
