package com.hmc.hmcbe.parking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parking.dto.request.ParkRequest;
import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;
import com.hmc.hmcbe.parking.entity.ParkingRecord;
import com.hmc.hmcbe.parking.exception.ParkingErrorCode;
import com.hmc.hmcbe.parking.fixture.ParkingRecordFixture;
import com.hmc.hmcbe.parking.repository.ParkingRecordRepository;
import com.hmc.hmcbe.parking.service.impl.ParkingRecordWriteServiceImpl;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ParkingRecordWriteServiceTest {

	private static final Instant NOW = Instant.parse("2026-11-09T07:00:00Z");

	@Mock
	private ParkingRecordRepository parkingRecordRepository;

	@Mock
	private ParkingLotReadService parkingLotReadService;

	private ParkingRecordWriteServiceImpl parkingRecordWriteService;

	@BeforeEach
	void setUp() {
		parkingRecordWriteService = new ParkingRecordWriteServiceImpl(parkingRecordRepository, parkingLotReadService,
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	@DisplayName("P단 주차를 현재 시각으로 기록한다")
	void park() {
		// given
		given(parkingRecordRepository.findByUserIdAndExitedAtIsNull(1L)).willReturn(Optional.empty());
		given(parkingRecordRepository.save(any(ParkingRecord.class))).willAnswer(i -> i.getArgument(0));

		// when
		ParkingRecordResponse response = parkingRecordWriteService.park(1L, new ParkRequest(37.5262, 126.929, 7L));

		// then
		verify(parkingLotReadService).getParkingLot(7L);
		assertThat(response.parkedAt()).isEqualTo(NOW);
		assertThat(response.parkingLotId()).isEqualTo(7L);
		assertThat(response.spotMemo()).isNull();
	}

	@Test
	@DisplayName("출차하지 않은 이전 기록이 있으면 먼저 출차·flush한 뒤 새로 기록한다")
	void parkExitsPrevious() {
		// given
		ParkingRecord previous = ParkingRecordFixture.activeRecord(1L);
		given(parkingRecordRepository.findByUserIdAndExitedAtIsNull(1L)).willReturn(Optional.of(previous));
		given(parkingRecordRepository.save(any(ParkingRecord.class))).willAnswer(i -> i.getArgument(0));

		// when
		parkingRecordWriteService.park(1L, new ParkRequest(37.5262, 126.929, null));

		// then
		assertThat(previous.getExitedAt()).isEqualTo(NOW);
		InOrder order = inOrder(parkingRecordRepository);
		order.verify(parkingRecordRepository).flush();
		order.verify(parkingRecordRepository).save(any(ParkingRecord.class));
	}

	@Test
	@DisplayName("없는 주차장으로 기록하면 NOT_FOUND이고 저장하지 않는다")
	void parkWithUnknownLot() {
		given(parkingLotReadService.getParkingLot(9L)).willThrow(new BusinessException(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND));

		BusinessException ex = assertThrows(BusinessException.class,
				() -> parkingRecordWriteService.park(1L, new ParkRequest(37.5262, 126.929, 9L)));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND);
		verify(parkingRecordRepository, never()).save(any());
	}

	@Test
	@DisplayName("층·구역을 저장한다")
	void updateSpotMemo() {
		ParkingRecord record = ParkingRecordFixture.withId(ParkingRecordFixture.activeRecord(1L), 3L);
		given(parkingRecordRepository.findById(3L)).willReturn(Optional.of(record));

		ParkingRecordResponse response = parkingRecordWriteService.updateSpotMemo(1L, 3L, "지하 2층 C구역");

		assertThat(response.spotMemo()).isEqualTo("지하 2층 C구역");
	}

	@Test
	@DisplayName("다른 사용자의 기록은 NOT_FOUND")
	void othersRecord() {
		ParkingRecord record = ParkingRecordFixture.withId(ParkingRecordFixture.activeRecord(2L), 3L);
		given(parkingRecordRepository.findById(3L)).willReturn(Optional.of(record));

		BusinessException ex = assertThrows(BusinessException.class,
				() -> parkingRecordWriteService.updateSpotMemo(1L, 3L, "지하 2층"));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingErrorCode.PARKING_RECORD_NOT_FOUND);
		assertThat(record.getSpotMemo()).isNull();
	}

	@Test
	@DisplayName("출차하면 현재 시각으로 기록을 끝낸다")
	void exit() {
		ParkingRecord record = ParkingRecordFixture.withId(ParkingRecordFixture.activeRecord(1L), 3L);
		given(parkingRecordRepository.findById(3L)).willReturn(Optional.of(record));

		parkingRecordWriteService.exit(1L, 3L);

		assertThat(record.getExitedAt()).isEqualTo(NOW);
	}
}
