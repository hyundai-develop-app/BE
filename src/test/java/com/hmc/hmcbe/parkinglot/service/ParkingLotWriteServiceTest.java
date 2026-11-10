package com.hmc.hmcbe.parkinglot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parkinglot.dto.request.RegisterParkingLotRequest;
import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import com.hmc.hmcbe.parkinglot.entity.ParkingFee;
import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import com.hmc.hmcbe.parkinglot.fixture.ParkingLotFixture;
import com.hmc.hmcbe.parkinglot.repository.OccupancyForecastRepository;
import com.hmc.hmcbe.parkinglot.repository.ParkingLotRepository;
import com.hmc.hmcbe.parkinglot.service.impl.ParkingLotWriteServiceImpl;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingLotWriteServiceTest {

	private static final Instant TARGET_AT = Instant.parse("2026-11-09T05:30:00Z");

	@Mock
	private ParkingLotRepository parkingLotRepository;

	@Mock
	private OccupancyForecastRepository occupancyForecastRepository;

	@InjectMocks
	private ParkingLotWriteServiceImpl parkingLotWriteService;

	@Test
	@DisplayName("주차장을 등록하면 요금 단위를 함께 저장한다")
	void register() {
		// given
		given(parkingLotRepository.save(any(ParkingLot.class)))
				.willAnswer(invocation -> invocation.getArgument(0));
		ArgumentCaptor<ParkingLot> captor = ArgumentCaptor.forClass(ParkingLot.class);

		// when
		parkingLotWriteService.register(request(10, 1000));

		// then
		verify(parkingLotRepository).save(captor.capture());
		assertThat(captor.getValue().getFee()).isEqualTo(new ParkingFee(10, 1000));
	}

	@Test
	@DisplayName("요금 단위 시간과 단위 요금 중 하나만 주면 PARKINGLOT4002")
	void registerWithPartialFee() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> parkingLotWriteService.register(request(10, null)));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_FEE);
		verify(parkingLotRepository, never()).save(any());
	}

	@Test
	@DisplayName("같은 시각의 예측이 있으면 만차율을 교체한다")
	void recordExistingForecast() {
		// given
		OccupancyForecast existing = ParkingLotFixture.forecast(1L, TARGET_AT, 50);
		given(parkingLotRepository.existsById(1L)).willReturn(true);
		given(occupancyForecastRepository.findByParkingLotIdAndTargetAt(1L, TARGET_AT))
				.willReturn(Optional.of(existing));

		// when
		parkingLotWriteService.recordForecast(1L, TARGET_AT, 90);

		// then
		assertThat(existing.getOccupancyRate()).isEqualTo(90);
		verify(occupancyForecastRepository, never()).save(any());
	}

	@Test
	@DisplayName("같은 시각의 예측이 없으면 새로 저장한다")
	void recordNewForecast() {
		given(parkingLotRepository.existsById(1L)).willReturn(true);
		given(occupancyForecastRepository.findByParkingLotIdAndTargetAt(1L, TARGET_AT)).willReturn(Optional.empty());

		parkingLotWriteService.recordForecast(1L, TARGET_AT, 90);

		verify(occupancyForecastRepository).save(any(OccupancyForecast.class));
	}

	@Test
	@DisplayName("없는 주차장의 예측을 저장하면 PARKINGLOT4041")
	void recordForecastOfUnknownLot() {
		given(parkingLotRepository.existsById(9L)).willReturn(false);

		BusinessException ex = assertThrows(BusinessException.class,
				() -> parkingLotWriteService.recordForecast(9L, TARGET_AT, 90));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND);
	}

	private static RegisterParkingLotRequest request(Integer feeUnitMinutes, Integer feeUnitPrice) {
		return new RegisterParkingLotRequest("여의도 공영주차장", 37.5259, 126.9284, true, null, 120,
				feeUnitMinutes, feeUnitPrice, 300);
	}
}
