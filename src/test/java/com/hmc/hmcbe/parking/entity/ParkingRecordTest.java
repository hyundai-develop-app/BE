package com.hmc.hmcbe.parking.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.parking.exception.ParkingErrorCode;
import com.hmc.hmcbe.parking.fixture.ParkingRecordFixture;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ParkingRecordTest {

	private static final Instant PARKED_AT = ParkingRecordFixture.PARKED_AT;

	@Test
	@DisplayName("P단 주차를 기록하면 GPS 위치와 주차 시각만 저장되고 층·구역은 비어 있다")
	void record() {
		ParkingRecord record = ParkingRecord.record(1L, 7L, ParkingRecordFixture.LOCATION, PARKED_AT);

		assertThat(record.getUserId()).isEqualTo(1L);
		assertThat(record.getParkingLotId()).isEqualTo(7L);
		assertThat(record.getLocation()).isEqualTo(ParkingRecordFixture.LOCATION);
		assertThat(record.getParkedAt()).isEqualTo(PARKED_AT);
		assertThat(record.getSpotMemo()).isNull();
		assertThat(record.isActive()).isTrue();
	}

	@Test
	@DisplayName("사용자·위치·주차 시각이 없으면 기록할 수 없다")
	void missingRequired() {
		assertThrows(BusinessException.class,
				() -> ParkingRecord.record(null, null, ParkingRecordFixture.LOCATION, PARKED_AT));
		assertThrows(BusinessException.class, () -> ParkingRecord.record(1L, null, null, PARKED_AT));
		assertThrows(BusinessException.class,
				() -> ParkingRecord.record(1L, null, ParkingRecordFixture.LOCATION, null));
	}

	@Test
	@DisplayName("층·구역을 말하면 저장하고, 다시 말하면 정정된다")
	void updateSpotMemo() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);

		record.updateSpotMemo("지하 2층 B구역");
		record.updateSpotMemo("지하 2층 C구역");

		assertThat(record.getSpotMemo()).isEqualTo("지하 2층 C구역");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" "})
	@DisplayName("빈 층·구역은 저장할 수 없다")
	void blankSpotMemo(String memo) {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);

		BusinessException ex = assertThrows(BusinessException.class, () -> record.updateSpotMemo(memo));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingErrorCode.INVALID_SPOT_MEMO);
	}

	@Test
	@DisplayName("100자를 넘는 층·구역은 저장할 수 없다")
	void tooLongSpotMemo() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);

		assertThrows(BusinessException.class, () -> record.updateSpotMemo("a".repeat(101)));
	}

	@Test
	@DisplayName("출차하면 출차 시각이 기록되고 더 이상 진행 중이 아니다")
	void exit() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);
		Instant exitAt = PARKED_AT.plus(Duration.ofMinutes(100));

		record.exit(exitAt);

		assertThat(record.getExitedAt()).isEqualTo(exitAt);
		assertThat(record.isActive()).isFalse();
	}

	@Test
	@DisplayName("주차 시각보다 이른 시각으로 출차할 수 없다")
	void exitBeforeParked() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);

		BusinessException ex = assertThrows(BusinessException.class, () -> record.exit(PARKED_AT.minusSeconds(1)));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingErrorCode.INVALID_EXIT_TIME);
	}

	@Test
	@DisplayName("출차한 기록은 다시 출차하거나 층·구역을 바꿀 수 없다")
	void exitedRecordIsImmutable() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);
		record.exit(PARKED_AT.plusSeconds(60));

		assertThat(assertThrows(BusinessException.class, () -> record.exit(PARKED_AT.plusSeconds(120)))
				.getErrorCode()).isEqualTo(ParkingErrorCode.ALREADY_EXITED);
		assertThat(assertThrows(BusinessException.class, () -> record.updateSpotMemo("지하 1층"))
				.getErrorCode()).isEqualTo(ParkingErrorCode.ALREADY_EXITED);
	}

	@Test
	@DisplayName("경과 시간은 주차 시각부터 기준 시각까지이고, 기준 시각이 더 이르면 0이다")
	void elapsed() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);

		assertThat(record.elapsed(PARKED_AT.plus(Duration.ofMinutes(100)))).isEqualTo(Duration.ofMinutes(100));
		assertThat(record.elapsed(PARKED_AT.minusSeconds(10))).isEqualTo(Duration.ZERO);
	}

	@Test
	@DisplayName("남은 무료 시간은 무료 시간에서 경과 시간을 뺀 값이고 0보다 작아지지 않는다")
	void remainingFreeTime() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);
		Duration freeTime = Duration.ofHours(2);

		assertThat(record.remainingFreeTime(freeTime, PARKED_AT.plus(Duration.ofMinutes(100))))
				.isEqualTo(Duration.ofMinutes(20));
		assertThat(record.remainingFreeTime(freeTime, PARKED_AT.plus(Duration.ofHours(3))))
				.isEqualTo(Duration.ZERO);
	}

	@Test
	@DisplayName("본인 기록인지 확인한다")
	void isOwnedBy() {
		ParkingRecord record = ParkingRecordFixture.activeRecord(1L);

		assertThat(record.isOwnedBy(1L)).isTrue();
		assertThat(record.isOwnedBy(2L)).isFalse();
	}
}
