package com.hmc.hmcbe.arrival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.hmc.hmcbe.arrival.dto.request.StartArrivalRequest;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;
import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import com.hmc.hmcbe.arrival.exception.ArrivalErrorCode;
import com.hmc.hmcbe.arrival.fixture.ArrivalSessionFixture;
import com.hmc.hmcbe.arrival.repository.ArrivalSessionRepository;
import com.hmc.hmcbe.arrival.service.impl.ArrivalWriteServiceImpl;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import com.hmc.hmcbe.parkinglot.fixture.NearbyParkingLotFixture;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArrivalWriteServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-09T05:00:00Z");

	@Mock
	private ArrivalSessionRepository arrivalSessionRepository;

	@Mock
	private ParkingLotReadService parkingLotReadService;

	private ArrivalWriteServiceImpl arrivalWriteService;

	@BeforeEach
	void setUp() {
		arrivalWriteService = new ArrivalWriteServiceImpl(arrivalSessionRepository, parkingLotReadService,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	@DisplayName("도착 모드를 시작하면 목적지 주차장을 확인하고 ACTIVE 세션을 저장한다")
	void start() {
		// given
		given(arrivalSessionRepository.findByUserIdAndStatus(1L, ArrivalStatus.ACTIVE)).willReturn(Optional.empty());
		given(arrivalSessionRepository.save(any(ArrivalSession.class))).willAnswer(i -> i.getArgument(0));

		// when
		ArrivalSessionResponse response = arrivalWriteService.start(1L, request(7L));

		// then
		verify(parkingLotReadService).getParkingLot(7L);
		verifyNoMoreInteractions(parkingLotReadService);
		assertThat(response.status()).isEqualTo(ArrivalStatus.ACTIVE);
		assertThat(response.destinationParkingLotId()).isEqualTo(7L);
	}

	@Test
	@DisplayName("진행 중인 세션이 있으면 먼저 취소·flush한 뒤 새 세션을 저장한다")
	void startReplacesActive() {
		// given
		ArrivalSession previous = ArrivalSessionFixture.activeSession(1L);
		given(arrivalSessionRepository.findByUserIdAndStatus(1L, ArrivalStatus.ACTIVE))
				.willReturn(Optional.of(previous));
		given(arrivalSessionRepository.save(any(ArrivalSession.class))).willAnswer(i -> i.getArgument(0));

		// when
		arrivalWriteService.start(1L, request(null));

		// then
		assertThat(previous.getStatus()).isEqualTo(ArrivalStatus.CANCELLED);
		assertThat(previous.getEndedAt()).isEqualTo(NOW);
		InOrder order = inOrder(arrivalSessionRepository);
		order.verify(arrivalSessionRepository).flush();
		order.verify(arrivalSessionRepository).save(any(ArrivalSession.class));
	}

	@Test
	@DisplayName("없는 목적지 주차장으로 시작하면 NOT_FOUND이고 세션을 저장하지 않는다")
	void startWithUnknownLot() {
		given(parkingLotReadService.getParkingLot(9L))
				.willThrow(new BusinessException(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND));

		BusinessException ex = assertThrows(BusinessException.class, () -> arrivalWriteService.start(1L, request(9L)));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND);
		verify(arrivalSessionRepository, never()).save(any());
	}

	@Test
	@DisplayName("경유 주차장을 선택하면 주차장을 확인하고 세션에 저장한다")
	void selectWaypoint() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		ArrivalSessionResponse response = arrivalWriteService.selectWaypoint(1L, 5L, 3L);

		verify(parkingLotReadService).getParkingLot(3L);
		assertThat(response.waypointParkingLotId()).isEqualTo(3L);
	}

	@Test
	@DisplayName("다른 사용자의 세션은 NOT_FOUND")
	void othersSession() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(2L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		BusinessException ex = assertThrows(BusinessException.class, () -> arrivalWriteService.cancel(1L, 5L));

		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.ARRIVAL_SESSION_NOT_FOUND);
		assertThat(session.getStatus()).isEqualTo(ArrivalStatus.ACTIVE);
	}

	@Test
	@DisplayName("완료하면 현재 시각으로 세션을 끝낸다")
	void complete() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		arrivalWriteService.complete(1L, 5L);

		assertThat(session.getStatus()).isEqualTo(ArrivalStatus.COMPLETED);
		assertThat(session.getEndedAt()).isEqualTo(NOW);
	}

	private static StartArrivalRequest request(Long destinationParkingLotId) {
		return new StartArrivalRequest("더현대 서울", 37.5259, 126.9284, destinationParkingLotId,
				ArrivalSessionFixture.ETA);
	}

	@Test
	@DisplayName("목적지 주차장 ID를 생략하면 목적지 반경 700m에서 가장 가까운 주차장을 저장하고 반환한다")
	void inferDestinationParkingLot() {
		// given
		NearbyParkingLotResponse nearest = NearbyParkingLotFixture.withoutForecast(7L, 600, 80);
		NearbyParkingLotResponse larger = NearbyParkingLotFixture.withoutForecast(3L, 650, 500);
		given(parkingLotReadService.findNearby(ArrivalSessionFixture.DESTINATION, 700, ArrivalSessionFixture.ETA))
				.willReturn(List.of(nearest, larger));
		given(arrivalSessionRepository.save(any(ArrivalSession.class))).willAnswer(i -> i.getArgument(0));

		// when
		ArrivalSessionResponse response = arrivalWriteService.start(1L, request(null));

		// then
		ArgumentCaptor<ArrivalSession> saved = ArgumentCaptor.forClass(ArrivalSession.class);
		verify(arrivalSessionRepository).save(saved.capture());
		assertThat(saved.getValue().getDestinationParkingLotId()).isEqualTo(7L);
		assertThat(saved.getValue().getDestinationLocation()).isEqualTo(ArrivalSessionFixture.DESTINATION);
		assertThat(saved.getValue().getExpectedArrivalAt()).isEqualTo(ArrivalSessionFixture.ETA);
		assertThat(response.destinationParkingLotId()).isEqualTo(7L);
		assertThat(response.waypointParkingLotId()).isNull();
		verify(parkingLotReadService).findNearby(ArrivalSessionFixture.DESTINATION, 700, ArrivalSessionFixture.ETA);
	}

	@Test
	@DisplayName("반경 700m 안에 주차장이 없으면 목적지 주차장 ID를 null로 저장하고 반환한다")
	void noDestinationParkingLotWithinRadius() {
		// given
		given(parkingLotReadService.findNearby(ArrivalSessionFixture.DESTINATION, 700, ArrivalSessionFixture.ETA))
				.willReturn(List.of());
		given(arrivalSessionRepository.save(any(ArrivalSession.class))).willAnswer(i -> i.getArgument(0));

		// when
		ArrivalSessionResponse response = arrivalWriteService.start(1L, request(null));

		// then
		ArgumentCaptor<ArrivalSession> saved = ArgumentCaptor.forClass(ArrivalSession.class);
		verify(arrivalSessionRepository).save(saved.capture());
		assertThat(saved.getValue().getDestinationParkingLotId()).isNull();
		assertThat(response.destinationParkingLotId()).isNull();
		assertThat(response.status()).isEqualTo(ArrivalStatus.ACTIVE);
		verify(parkingLotReadService).findNearby(ArrivalSessionFixture.DESTINATION, 700, ArrivalSessionFixture.ETA);
	}

	@Test
	@DisplayName("본인 세션의 경유지를 해제하면 세션에도 반영하고 목적지를 유지한 응답을 반환한다")
	void clearWaypoint() {
		// given
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L, 7L), 5L);
		session.selectWaypoint(3L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		// when
		ArrivalSessionResponse response = arrivalWriteService.clearWaypoint(1L, 5L);

		// then
		assertThat(session.getWaypointParkingLotId()).isNull();
		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.waypointParkingLotId()).isNull();
		assertThat(response.destinationParkingLotId()).isEqualTo(7L);
		assertThat(response.status()).isEqualTo(ArrivalStatus.ACTIVE);
		verifyNoInteractions(parkingLotReadService);
	}

	@Test
	@DisplayName("경유지 해제를 반복하면 경유지가 null인 ACTIVE 세션 응답을 반환한다")
	void clearWaypointTwice() {
		// given
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		// when
		ArrivalSessionResponse first = arrivalWriteService.clearWaypoint(1L, 5L);
		ArrivalSessionResponse second = arrivalWriteService.clearWaypoint(1L, 5L);

		// then
		assertThat(first.waypointParkingLotId()).isNull();
		assertThat(second).isEqualTo(first);
		assertThat(second.status()).isEqualTo(ArrivalStatus.ACTIVE);
	}

	@Test
	@DisplayName("다른 사용자의 경유지 해제를 요청하면 NOT_FOUND이고 경유지를 유지한다")
	void clearOthersWaypoint() {
		// given
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(2L, 7L), 5L);
		session.selectWaypoint(3L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		// when
		BusinessException ex = assertThrows(BusinessException.class, () -> arrivalWriteService.clearWaypoint(1L, 5L));

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.ARRIVAL_SESSION_NOT_FOUND);
		assertThat(session.getWaypointParkingLotId()).isEqualTo(3L);
		assertThat(session.getStatus()).isEqualTo(ArrivalStatus.ACTIVE);
		verifyNoInteractions(parkingLotReadService);
	}

	@Test
	@DisplayName("없는 세션의 경유지 해제를 요청하면 NOT_FOUND로 거절한다")
	void clearWaypointOfMissingSession() {
		// given
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.empty());

		// when
		BusinessException ex = assertThrows(BusinessException.class, () -> arrivalWriteService.clearWaypoint(1L, 5L));

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.ARRIVAL_SESSION_NOT_FOUND);
		verifyNoInteractions(parkingLotReadService);
	}

	@Test
	@DisplayName("종료한 본인 세션의 경유지를 해제하면 ALREADY_ENDED로 거절한다")
	void clearWaypointOfEndedSession() {
		// given
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L), 5L);
		session.selectWaypoint(3L);
		session.cancel(NOW);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		// when
		BusinessException ex = assertThrows(BusinessException.class, () -> arrivalWriteService.clearWaypoint(1L, 5L));

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.ALREADY_ENDED);
		assertThat(session.getWaypointParkingLotId()).isEqualTo(3L);
		verifyNoInteractions(parkingLotReadService);
	}
}
