package com.hmc.hmcbe.arrival.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.hmc.hmcbe.arrival.dto.response.ArrivalOutlookResponse;
import com.hmc.hmcbe.arrival.dto.response.ParkingBriefingResponse;
import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import com.hmc.hmcbe.arrival.exception.ArrivalErrorCode;
import com.hmc.hmcbe.arrival.fixture.ArrivalSessionFixture;
import com.hmc.hmcbe.arrival.repository.ArrivalSessionRepository;
import com.hmc.hmcbe.arrival.service.impl.ArrivalReadServiceImpl;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArrivalReadServiceTest {

	private static final Instant ETA = ArrivalSessionFixture.ETA;

	@Mock
	private ArrivalSessionRepository arrivalSessionRepository;

	@Mock
	private ParkingLotReadService parkingLotReadService;

	private ArrivalReadServiceImpl arrivalReadService;

	private final NearbyParkingLotResponse destinationLot = candidate(7L, 0, 95, "CONGESTED");
	private final NearbyParkingLotResponse publicLot = candidate(3L, 300, 40, "AVAILABLE");

	@BeforeEach
	void setUp() {
		arrivalReadService = new ArrivalReadServiceImpl(arrivalSessionRepository, parkingLotReadService,
				new ParkingRecommendationPolicy());
	}

	@Test
	@DisplayName("대기 화면은 목적지 주차장 도착 예측, 도착 전후 1시간 예측, 더 여유 있는 주차장 수를 준다")
	void getOutlook() {
		// given
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L, 7L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));
		given(parkingLotReadService.findNearby(ArrivalSessionFixture.DESTINATION, 500, ETA))
				.willReturn(List.of(destinationLot, publicLot));
		List<OccupancyForecastResponse> chart = List.of(new OccupancyForecastResponse(ETA, 95, "CONGESTED"));
		given(parkingLotReadService.getForecasts(7L, ETA.minus(Duration.ofHours(1)), ETA.plus(Duration.ofHours(1))))
				.willReturn(chart);

		// when
		ArrivalOutlookResponse outlook = arrivalReadService.getOutlook(1L, 5L);

		// then
		assertThat(outlook.session().id()).isEqualTo(5L);
		assertThat(outlook.destinationParkingLot()).isEqualTo(destinationLot);
		assertThat(outlook.destinationForecasts()).isEqualTo(chart);
		assertThat(outlook.betterAlternativeCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("목적지 주차장이 없으면 시간대별 예측을 조회하지 않는다")
	void getOutlookWithoutDestinationLot() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));
		given(parkingLotReadService.findNearby(ArrivalSessionFixture.DESTINATION, 500, ETA))
				.willReturn(List.of(publicLot));

		ArrivalOutlookResponse outlook = arrivalReadService.getOutlook(1L, 5L);

		assertThat(outlook.destinationParkingLot()).isNull();
		assertThat(outlook.destinationForecasts()).isEmpty();
		verify(parkingLotReadService).findNearby(ArrivalSessionFixture.DESTINATION, 500, ETA);
		verifyNoMoreInteractions(parkingLotReadService);
	}

	@Test
	@DisplayName("브리핑은 목적지 주차장과 추천 주차장을 준다")
	void getBriefing() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L, 7L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));
		given(parkingLotReadService.findNearby(ArrivalSessionFixture.DESTINATION, 500, ETA))
				.willReturn(List.of(destinationLot, publicLot));

		ParkingBriefingResponse briefing = arrivalReadService.getBriefing(1L, 5L);

		assertThat(briefing.destinationParkingLot()).isEqualTo(destinationLot);
		assertThat(briefing.recommended()).isEqualTo(publicLot);
		assertThat(briefing.alternatives()).containsExactly(publicLot);
	}

	@Test
	@DisplayName("목적지 주차장이 500m 밖에 있어도 ID로 조회해 그보다 여유 있는 곳만 추천한다")
	void compareDestinationOutsideSearchRadius() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L, 7L), 5L);
		NearbyParkingLotResponse distantDestination = candidate(7L, 1500, 20, "AVAILABLE");
		NearbyParkingLotResponse better = candidate(4L, 250, 10, "AVAILABLE");
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));
		given(parkingLotReadService.findNearby(ArrivalSessionFixture.DESTINATION, 500, ETA))
				.willReturn(List.of(publicLot, better));
		given(parkingLotReadService.getNearbyById(7L, ArrivalSessionFixture.DESTINATION, ETA))
				.willReturn(distantDestination);

		ParkingBriefingResponse briefing = arrivalReadService.getBriefing(1L, 5L);

		assertThat(briefing.destinationParkingLot()).isEqualTo(distantDestination);
		assertThat(briefing.alternatives()).containsExactly(better);
		assertThat(briefing.recommended()).isEqualTo(better);
	}

	@Test
	@DisplayName("가까운 10개에서 목적지 주차장이 빠져도 목적지 예측을 대기 화면에 반환한다")
	void getOutlookWhenDestinationExcludedByLimit() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(1L, 7L), 5L);
		List<NearbyParkingLotResponse> nearby = java.util.stream.LongStream.rangeClosed(10, 19)
				.mapToObj(id -> candidate(id, 100, 40, "AVAILABLE")).toList();
		NearbyParkingLotResponse omittedDestination = candidate(7L, 400, 20, "AVAILABLE");
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));
		given(parkingLotReadService.findNearby(ArrivalSessionFixture.DESTINATION, 500, ETA)).willReturn(nearby);
		given(parkingLotReadService.getNearbyById(7L, ArrivalSessionFixture.DESTINATION, ETA))
				.willReturn(omittedDestination);

		ArrivalOutlookResponse outlook = arrivalReadService.getOutlook(1L, 5L);

		assertThat(outlook.destinationParkingLot()).isEqualTo(omittedDestination);
		assertThat(outlook.betterAlternativeCount()).isZero();
	}

	@Test
	@DisplayName("다른 사용자의 세션 브리핑은 NOT_FOUND")
	void getOthersBriefing() {
		ArrivalSession session = ArrivalSessionFixture.withId(ArrivalSessionFixture.activeSession(2L), 5L);
		given(arrivalSessionRepository.findById(5L)).willReturn(Optional.of(session));

		BusinessException ex = assertThrows(BusinessException.class, () -> arrivalReadService.getBriefing(1L, 5L));

		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.ARRIVAL_SESSION_NOT_FOUND);
	}

	@Test
	@DisplayName("진행 중인 세션이 없으면 NOT_FOUND")
	void noActiveSession() {
		given(arrivalSessionRepository.findByUserIdAndStatus(1L, ArrivalStatus.ACTIVE)).willReturn(Optional.empty());

		BusinessException ex = assertThrows(BusinessException.class, () -> arrivalReadService.getActiveSession(1L));

		assertThat(ex.getErrorCode()).isEqualTo(ArrivalErrorCode.ACTIVE_ARRIVAL_SESSION_NOT_FOUND);
	}

	private static NearbyParkingLotResponse candidate(Long id, int distance, int rate, String level) {
		ParkingLotResponse lot = new ParkingLotResponse(id, "주차장" + id, 37.5, 126.9, true, null, 120, 10, 1000, 100);
		return new NearbyParkingLotResponse(lot, distance, new OccupancyForecastResponse(ETA, rate, level));
	}
}
