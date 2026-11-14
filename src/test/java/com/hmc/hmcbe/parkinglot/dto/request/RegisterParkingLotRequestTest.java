package com.hmc.hmcbe.parkinglot.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class RegisterParkingLotRequestTest {

	private static ValidatorFactory validatorFactory;
	private static Validator validator;

	@BeforeAll
	static void createValidator() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void closeValidator() {
		validatorFactory.close();
	}

	@ParameterizedTest
	@NullSource
	@ValueSource(booleans = {false, true})
	@DisplayName("새 필드를 생략하거나 주말 전용 여부를 지정하면 요청 검증을 통과한다")
	void optionalFrontFields(Boolean weekendOnly) {
		// given
		RegisterParkingLotRequest request = request(null, null, weekendOnly);

		// when & then
		assertThat(validator.validate(request)).isEmpty();
		assertThat(request.address()).isNull();
		assertThat(request.feeDescription()).isNull();
	}

	@Test
	@DisplayName("주소와 요금 문구가 255자이면 요청 검증을 통과한다")
	void maximumLengthFrontFields() {
		// given
		RegisterParkingLotRequest request = request("가".repeat(255), "나".repeat(255), true);

		// when & then
		assertThat(validator.validate(request)).isEmpty();
	}

	@Test
	@DisplayName("주소가 256자이면 address 필드의 요청 검증에 실패한다")
	void tooLongAddress() {
		// given
		RegisterParkingLotRequest request = request("가".repeat(256), null, null);

		// when & then
		assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
				.containsExactly("address");
	}

	@Test
	@DisplayName("요금 문구가 256자이면 feeDescription 필드의 요청 검증에 실패한다")
	void tooLongFeeDescription() {
		// given
		RegisterParkingLotRequest request = request(null, "가".repeat(256), null);

		// when & then
		assertThat(validator.validate(request)).extracting(v -> v.getPropertyPath().toString())
				.containsExactly("feeDescription");
	}

	private static RegisterParkingLotRequest request(String address, String feeDescription, Boolean weekendOnly) {
		return new RegisterParkingLotRequest("주차장", 37.5259, 126.9284, true, null, 0, null, null, null,
				address, feeDescription, weekendOnly);
	}
}
