package com.hmc.hmcbe.parkinglot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import com.hmc.hmcbe.parkinglot.fixture.ParkingLotFixture;
import com.hmc.hmcbe.parkinglot.repository.NearbyParkingLotProjection;
import com.hmc.hmcbe.parkinglot.repository.OccupancyForecastRepository;
import com.hmc.hmcbe.parkinglot.repository.ParkingLotRepository;
import com.hmc.hmcbe.parkinglot.service.impl.ParkingLotReadServiceImpl;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingLotReadServiceTest {

	private static final Instant AT = Instant.parse("2026-11-09T05:40:00Z");

	@Mock
	private ParkingLotRepository parkingLotRepository;

	@Mock
	private OccupancyForecastRepository occupancyForecastRepository;

	@InjectMocks
	private ParkingLotReadServiceImpl parkingLotReadService;

	@Test
	@DisplayName("주변 주차장을 거리 순으로, 기준 시각 슬롯의 가장 늦은 예측과 함께 반환한다")
	void findNearby() {
		// given
		given(parkingLotRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.willReturn(List.of(projection(2L, 120.4), projection(1L, 300.6)));
		given(parkingLotRepository.findAllById(List.of(2L, 1L)))
				.willReturn(List.of(ParkingLotFixture.parkingLotWithId(1L), ParkingLotFixture.parkingLotWithId(2L)));
		given(occupancyForecastRepository.findByParkingLotIdInAndTargetAtBetween(eq(List.of(2L, 1L)), any(), eq(AT)))
				.willReturn(List.of(
						ParkingLotFixture.forecast(1L, Instant.parse("2026-11-09T05:30:00Z"), 95),
						ParkingLotFixture.forecast(1L, Instant.parse("2026-11-09T05:15:00Z"), 50)));

		// when
		List<NearbyParkingLotResponse> result =
				parkingLotReadService.findNearby(ParkingLotFixture.YEOUIDO, 500, AT);

		// then
		assertThat(result).extracting(r -> r.parkingLot().id()).containsExactly(2L, 1L);
		assertThat(result).extracting(NearbyParkingLotResponse::distanceMeters).containsExactly(120, 301);
		assertThat(result.get(0).forecast()).isNull();
		assertThat(result.get(1).forecast().occupancyRate()).isEqualTo(95);
		assertThat(result.get(1).forecast().congestionLevel()).isEqualTo("CONGESTED");
	}

	@Test
	@DisplayName("ID로 주차장을 조회하면 거리와 기준 시각 슬롯의 가장 늦은 예측을 반환한다")
	void findNearbyByIdWithForecast() {
		given(parkingLotRepository.findById(1L)).willReturn(Optional.of(ParkingLotFixture.parkingLotWithId(1L)));
		given(parkingLotRepository.findNearbyById(1L, ParkingLotFixture.YEOUIDO.latitude(),
				ParkingLotFixture.YEOUIDO.longitude())).willReturn(Optional.of(projection(1L, 1500.6)));
		Instant slotStart = AT.minusSeconds(1800).plusNanos(1);
		given(occupancyForecastRepository.findByParkingLotIdInAndTargetAtBetween(List.of(1L), slotStart, AT))
				.willReturn(List.of(ParkingLotFixture.forecast(1L, AT.minusSeconds(600), 20),
						ParkingLotFixture.forecast(1L, AT.minusSeconds(1200), 80)));

		NearbyParkingLotResponse result = parkingLotReadService.findNearbyById(1L, ParkingLotFixture.YEOUIDO, AT).orElseThrow();

		assertThat(result.parkingLot().id()).isEqualTo(1L);
		assertThat(result.distanceMeters()).isEqualTo(1501);
		assertThat(result.forecast().occupancyRate()).isEqualTo(20);
		verify(occupancyForecastRepository).findByParkingLotIdInAndTargetAtBetween(List.of(1L), slotStart, AT);
	}

	@Test
	@DisplayName("ID로 조회한 주차장의 기준 시각 예측이 없으면 forecast는 null이다")
	void findNearbyByIdWithoutForecast() {
		given(parkingLotRepository.findById(1L)).willReturn(Optional.of(ParkingLotFixture.parkingLotWithId(1L)));
		given(parkingLotRepository.findNearbyById(1L, ParkingLotFixture.YEOUIDO.latitude(),
				ParkingLotFixture.YEOUIDO.longitude())).willReturn(Optional.of(projection(1L, 1500)));

		NearbyParkingLotResponse result = parkingLotReadService.findNearbyById(1L, ParkingLotFixture.YEOUIDO, AT).orElseThrow();

		assertThat(result.forecast()).isNull();
	}

	@Test
	@DisplayName("주변에 주차장이 없으면 예측을 조회하지 않고 빈 목록을 반환한다")
	void findNearbyEmpty() {
		given(parkingLotRepository.findNearby(anyDouble(), anyDouble(), anyDouble(), anyInt())).willReturn(List.of());

		assertThat(parkingLotReadService.findNearby(ParkingLotFixture.YEOUIDO, 500, AT)).isEmpty();
		verifyNoInteractions(occupancyForecastRepository);
	}

	@Test
	@DisplayName("검색 반경이 1 ~ 5000m를 벗어나면 PARKINGLOT4004")
	void invalidRadius() {
		assertThat(assertThrows(BusinessException.class,
				() -> parkingLotReadService.findNearby(ParkingLotFixture.YEOUIDO, 0, AT)).getErrorCode())
				.isEqualTo(ParkingLotErrorCode.INVALID_SEARCH_RADIUS);
		assertThat(assertThrows(BusinessException.class,
				() -> parkingLotReadService.findNearby(ParkingLotFixture.YEOUIDO, 5001, AT)).getErrorCode())
				.isEqualTo(ParkingLotErrorCode.INVALID_SEARCH_RADIUS);
	}

	@Test
	@DisplayName("예측 만차율을 구간으로 조회한다")
	void getForecasts() {
		// given
		Instant from = Instant.parse("2026-11-09T04:30:00Z");
		Instant to = Instant.parse("2026-11-09T06:00:00Z");
		given(parkingLotRepository.findById(1L)).willReturn(Optional.of(ParkingLotFixture.parkingLotWithId(1L)));
		given(occupancyForecastRepository.findByParkingLotIdAndTargetAtBetweenOrderByTargetAtAsc(1L, from, to))
				.willReturn(List.of(ParkingLotFixture.forecast(1L, from, 58), ParkingLotFixture.forecast(1L, to, 82)));

		// when
		List<OccupancyForecastResponse> result = parkingLotReadService.getForecasts(1L, from, to);

		// then
		assertThat(result).extracting(OccupancyForecastResponse::occupancyRate).containsExactly(58, 82);
	}

	@Test
	@DisplayName("시작 시각이 끝 시각보다 늦으면 PARKINGLOT4005")
	void getForecastsInvalidRange() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> parkingLotReadService.getForecasts(1L, AT, AT.minusSeconds(1)));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_FORECAST_PERIOD);
	}

	@Test
	@DisplayName("없는 주차장을 조회하면 PARKINGLOT4041")
	void getUnknownParkingLot() {
		given(parkingLotRepository.findById(9L)).willReturn(Optional.empty());

		BusinessException ex = assertThrows(BusinessException.class, () -> parkingLotReadService.getParkingLot(9L));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND);
	}

	private static NearbyParkingLotProjection projection(Long id, double distance) {
		return new NearbyParkingLotProjection() {
			@Override
			public Long getId() {
				return id;
			}

			@Override
			public Double getDistanceMeters() {
				return distance;
			}
		};
	}
}
