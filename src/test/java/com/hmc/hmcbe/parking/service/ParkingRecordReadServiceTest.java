package com.hmc.hmcbe.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.parking.dto.response.ParkingSummaryResponse;
import com.hmc.hmcbe.parking.fixture.ParkingRecordFixture;
import com.hmc.hmcbe.parking.repository.ParkingRecordRepository;
import com.hmc.hmcbe.parking.service.impl.ParkingRecordReadServiceImpl;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.place.service.SavedPlaceReadService;
import java.time.Clock;
import java.time.Duration;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingRecordReadServiceTest {

	@Mock
	private ParkingRecordRepository parkingRecordRepository;

	@Mock
	private ParkingLotReadService parkingLotReadService;

	@Mock
	private SavedPlaceReadService savedPlaceReadService;

	private ParkingRecordReadServiceImpl parkingRecordReadService;

	private final List<SavedPlaceResponse> places = List.of(
			new SavedPlaceResponse(1L, "집", 37.49, 127.02), new SavedPlaceResponse(2L, "회사", 37.51, 127.05));

	@BeforeEach
	void setUp() {
		// 주차 후 1시간 40분 경과 시점
		Clock clock = Clock.fixed(ParkingRecordFixture.PARKED_AT.plus(Duration.ofMinutes(100)), ZoneOffset.UTC);
		parkingRecordReadService = new ParkingRecordReadServiceImpl(parkingRecordRepository, parkingLotReadService,
				savedPlaceReadService, clock);
	}

	@Test
	@DisplayName("요약은 경과 1시간 40분, 무료 2시간이면 남은 무료 20분과 빠른 목적지를 준다")
	void getSummary() {
		// given
		given(parkingRecordRepository.findByUserIdAndExitedAtIsNull(1L))
				.willReturn(Optional.of(ParkingRecordFixture.activeRecord(1L, 7L)));
		given(parkingLotReadService.getParkingLot(7L)).willReturn(
				new ParkingLotResponse(7L, "여의도 공영주차장", 37.5, 126.9, true, null, 120, 10, 1000, 300));
		given(savedPlaceReadService.getSavedPlaces(1L)).willReturn(places);

		// when
		ParkingSummaryResponse summary = parkingRecordReadService.getSummary(1L);

		// then
		assertThat(summary.parkingLotName()).isEqualTo("여의도 공영주차장");
		assertThat(summary.elapsedMinutes()).isEqualTo(100);
		assertThat(summary.freeMinutes()).isEqualTo(120);
		assertThat(summary.remainingFreeMinutes()).isEqualTo(20);
		assertThat(summary.quickDestinations()).isEqualTo(places);
	}

	@Test
	@DisplayName("주차장을 모르면 무료 시간 정보 없이 경과 시간만 준다")
	void getSummaryWithoutLot() {
		given(parkingRecordRepository.findByUserIdAndExitedAtIsNull(1L))
				.willReturn(Optional.of(ParkingRecordFixture.activeRecord(1L)));
		given(savedPlaceReadService.getSavedPlaces(1L)).willReturn(places);

		ParkingSummaryResponse summary = parkingRecordReadService.getSummary(1L);

		assertThat(summary.parkingLotName()).isNull();
		assertThat(summary.freeMinutes()).isNull();
		assertThat(summary.remainingFreeMinutes()).isNull();
		assertThat(summary.elapsedMinutes()).isEqualTo(100);
		verifyNoInteractions(parkingLotReadService);
	}

	@Test
	@DisplayName("진행 중인 주차 기록이 없으면 NOT_FOUND")
	void noActiveRecord() {
		given(parkingRecordRepository.findByUserIdAndExitedAtIsNull(1L)).willReturn(Optional.empty());

		BusinessException ex = assertThrows(BusinessException.class, () -> parkingRecordReadService.getSummary(1L));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
	}
}
