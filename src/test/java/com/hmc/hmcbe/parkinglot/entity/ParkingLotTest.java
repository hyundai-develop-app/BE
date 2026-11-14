package com.hmc.hmcbe.parkinglot.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import com.hmc.hmcbe.parkinglot.fixture.ParkingLotFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ParkingLotTest {

	private static final GeoPoint LOCATION = new GeoPoint(37.5259, 126.9284);

	@Test
	@DisplayName("주차장을 만들면 위치·실내 여부·무료 시간·요금이 저장된다")
	void create() {
		// when
		ParkingLot lot = ParkingLot.create("여의도 공영주차장", LOCATION, true, "건물 뒷편 골목",
				120, new ParkingFee(10, 1000), 300, null, null, false);

		// then
		assertThat(lot.getName()).isEqualTo("여의도 공영주차장");
		assertThat(lot.getLocation()).isEqualTo(LOCATION);
		assertThat(lot.isIndoor()).isTrue();
		assertThat(lot.getEntranceGuide()).isEqualTo("건물 뒷편 골목");
		assertThat(lot.getFreeMinutes()).isEqualTo(120);
		assertThat(lot.getFee()).isEqualTo(new ParkingFee(10, 1000));
		assertThat(lot.getTotalSpaces()).isEqualTo(300);
	}

	@Test
	@DisplayName("요금·입구 안내·총 면수는 없어도 된다")
	void createWithoutOptional() {
		ParkingLot lot = ParkingLot.create("무료 주차장", LOCATION, false, null, 0, null, null, null, null, false);

		assertThat(lot.getFee()).isNull();
		assertThat(lot.getEntranceGuide()).isNull();
		assertThat(lot.getTotalSpaces()).isNull();
		assertThat(lot.getAddress()).isNull();
		assertThat(lot.getFeeDescription()).isNull();
		assertThat(lot.isWeekendOnly()).isFalse();
	}

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	@DisplayName("주소·자유 요금 문구·주말 전용 여부를 지정하면 그대로 저장한다")
	void createWithFrontFields(boolean weekendOnly) {
		// given
		String address = "서울 강남구 영동대로 513 지하";
		String feeDescription = "최초 2시간 3,000원";

		// when
		ParkingLot lot = ParkingLotFixture.parkingLot("주차장", LOCATION, address, feeDescription, weekendOnly);

		// then
		assertThat(lot.getAddress()).isEqualTo(address);
		assertThat(lot.getFeeDescription()).isEqualTo(feeDescription);
		assertThat(lot.isWeekendOnly()).isEqualTo(weekendOnly);
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 255})
	@DisplayName("주소와 요금 문구가 1자 또는 최대 255자이면 만들 수 있다")
	void frontFieldLengthBoundaries(int length) {
		// given
		String text = "가".repeat(length);

		// when
		ParkingLot lot = ParkingLotFixture.parkingLot("주차장", LOCATION, text, text, false);

		// then
		assertThat(lot.getAddress()).isEqualTo(text);
		assertThat(lot.getFeeDescription()).isEqualTo(text);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "\t", "\n", " \t\n "})
	@DisplayName("주소가 빈 문자열이나 공백뿐이면 PARKINGLOT4001로 거부한다")
	void blankAddress(String address) {
		// when
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLotFixture.parkingLot("주차장", LOCATION, address, null, false));

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@Test
	@DisplayName("주소가 256자이면 PARKINGLOT4001로 거부한다")
	void tooLongAddress() {
		// given
		String address = "가".repeat(256);

		// when
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLotFixture.parkingLot("주차장", LOCATION, address, null, false));

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@ParameterizedTest
	@ValueSource(strings = {"", " ", "\t", "\n", " \t\n "})
	@DisplayName("요금 문구가 빈 문자열이나 공백뿐이면 PARKINGLOT4001로 거부한다")
	void blankFeeDescription(String feeDescription) {
		// when
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLotFixture.parkingLot("주차장", LOCATION, null, feeDescription, false));

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@Test
	@DisplayName("요금 문구가 256자이면 PARKINGLOT4001로 거부한다")
	void tooLongFeeDescription() {
		// given
		String feeDescription = "가".repeat(256);

		// when
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLotFixture.parkingLot("주차장", LOCATION, null, feeDescription, false));

		// then
		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" "})
	@DisplayName("이름이 비어 있으면 만들 수 없다")
	void blankName(String name) {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLot.create(name, LOCATION, false, null, 0, null, null, null, null, false));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@Test
	@DisplayName("이름이 100자를 넘으면 만들 수 없다")
	void tooLongName() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLot.create("a".repeat(101), LOCATION, false, null, 0, null, null, null, null, false));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@Test
	@DisplayName("위치가 없으면 만들 수 없다")
	void nullLocation() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLot.create("주차장", null, false, null, 0, null, null, null, null, false));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@Test
	@DisplayName("무료 시간이 음수면 만들 수 없다")
	void negativeFreeMinutes() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLot.create("주차장", LOCATION, false, null, -1, null, null, null, null, false));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1})
	@DisplayName("총 면수가 1 미만이면 만들 수 없다")
	void invalidTotalSpaces(int totalSpaces) {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLot.create("주차장", LOCATION, false, null, 0, null, totalSpaces, null, null, false));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@Test
	@DisplayName("입구 안내가 255자를 넘으면 만들 수 없다")
	void tooLongEntranceGuide() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> ParkingLot.create("주차장", LOCATION, false, "a".repeat(256), 0, null, null, null, null, false));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_PARKING_LOT);
	}

	@ParameterizedTest
	@CsvSource({"0, 1000", "-10, 1000", "10, -1"})
	@DisplayName("요금 단위 시간이 1분 미만이거나 단위 요금이 음수면 요금을 만들 수 없다")
	void invalidFee(int unitMinutes, int unitPrice) {
		BusinessException ex = assertThrows(BusinessException.class, () -> new ParkingFee(unitMinutes, unitPrice));

		assertThat(ex.getErrorCode()).isEqualTo(ParkingLotErrorCode.INVALID_FEE);
	}
}
