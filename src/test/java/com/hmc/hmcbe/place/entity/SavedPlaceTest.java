package com.hmc.hmcbe.place.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.global.geo.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SavedPlaceTest {

	private static final GeoPoint HOME = new GeoPoint(37.4979, 127.0276);

	@Test
	@DisplayName("사용자의 장소를 이름과 위치로 저장한다")
	void create() {
		SavedPlace place = SavedPlace.create(1L, "집", HOME);

		assertThat(place.getUserId()).isEqualTo(1L);
		assertThat(place.getName()).isEqualTo("집");
		assertThat(place.getLocation()).isEqualTo(HOME);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" "})
	@DisplayName("이름이 비어 있으면 저장할 수 없다")
	void blankName(String name) {
		BusinessException ex = assertThrows(BusinessException.class, () -> SavedPlace.create(1L, name, HOME));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("이름이 50자를 넘으면 저장할 수 없다")
	void tooLongName() {
		BusinessException ex = assertThrows(BusinessException.class,
				() -> SavedPlace.create(1L, "a".repeat(51), HOME));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("사용자나 위치가 없으면 저장할 수 없다")
	void missingRequired() {
		assertThat(assertThrows(BusinessException.class, () -> SavedPlace.create(null, "집", HOME)).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
		assertThat(assertThrows(BusinessException.class, () -> SavedPlace.create(1L, "집", null)).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_REQUEST);
	}

	@Test
	@DisplayName("본인 장소인지 확인한다")
	void isOwnedBy() {
		SavedPlace place = SavedPlace.create(1L, "집", HOME);

		assertThat(place.isOwnedBy(1L)).isTrue();
		assertThat(place.isOwnedBy(2L)).isFalse();
	}
}
