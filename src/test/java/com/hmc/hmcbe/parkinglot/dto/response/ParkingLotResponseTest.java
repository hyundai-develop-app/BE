package com.hmc.hmcbe.parkinglot.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import com.hmc.hmcbe.parkinglot.fixture.ParkingLotFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ParkingLotResponseTest {

	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	@DisplayName("주차장을 응답으로 바꾸면 주소·요금 문구·주말 전용 여부와 기존 단위 요금을 함께 반환한다")
	void fromWithFrontFields(boolean weekendOnly) {
		// given
		ParkingLot lot = ParkingLotFixture.parkingLotWithId(7L,
				"서울 강남구 영동대로 513 지하", "최초 2시간 3,000원", weekendOnly);

		// when
		ParkingLotResponse response = ParkingLotResponse.from(lot);

		// then
		assertThat(response.id()).isEqualTo(7L);
		assertThat(response.address()).isEqualTo(lot.getAddress());
		assertThat(response.feeDescription()).isEqualTo(lot.getFeeDescription());
		assertThat(response.weekendOnly()).isEqualTo(weekendOnly);
		assertThat(response.feeUnitMinutes()).isEqualTo(10);
		assertThat(response.feeUnitPrice()).isEqualTo(1000);
	}

	@ParameterizedTest
	@CsvSource(value = {"NULL, NULL", "서울 강남구, NULL", "NULL, 주말·공휴일 운영 시간 내 무료"}, nullValues = "NULL")
	@DisplayName("주소·요금 문구·단위 요금이 없으면 응답에서도 각각 null을 유지한다")
	void fromWithOptionalFrontFields(String address, String feeDescription) {
		// given
		ParkingLot lot = ParkingLot.create("주차장", ParkingLotFixture.YEOUIDO, false, null, 0, null, null,
				address, feeDescription, false);

		// when
		ParkingLotResponse response = ParkingLotResponse.from(lot);

		// then
		assertThat(response.address()).isEqualTo(address);
		assertThat(response.feeDescription()).isEqualTo(feeDescription);
		assertThat(response.weekendOnly()).isFalse();
		assertThat(response.feeUnitMinutes()).isNull();
		assertThat(response.feeUnitPrice()).isNull();
	}
}
