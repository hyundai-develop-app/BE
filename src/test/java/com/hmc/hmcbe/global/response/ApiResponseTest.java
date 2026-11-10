package com.hmc.hmcbe.global.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.hmc.hmcbe.global.exception.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApiResponseTest {

	@Test
	@DisplayName("데이터와 함께 성공 응답을 만들면 success는 true이고 error는 없다")
	void okWithData() {
		// given
		String data = "payload";

		// when
		ApiResponse<String> response = ApiResponse.ok(data);

		// then
		assertThat(response.success()).isTrue();
		assertThat(response.data()).isEqualTo(data);
		assertThat(response.error()).isNull();
	}

	@Test
	@DisplayName("데이터 없이 성공 응답을 만들면 data와 error가 모두 없다")
	void okWithoutData() {
		// when
		ApiResponse<Void> response = ApiResponse.ok();

		// then
		assertThat(response.success()).isTrue();
		assertThat(response.data()).isNull();
		assertThat(response.error()).isNull();
	}

	@Test
	@DisplayName("실패 응답을 만들면 error에 ErrorCode의 코드와 메시지가 담긴다")
	void fail() {
		// when
		ApiResponse<Void> response = ApiResponse.fail(CommonErrorCode.NOT_FOUND, "없음");

		// then
		assertThat(response.success()).isFalse();
		assertThat(response.data()).isNull();
		assertThat(response.error().code()).isEqualTo("COMMON4041");
		assertThat(response.error().message()).isEqualTo("없음");
	}
}
