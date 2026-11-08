package com.hmc.hmcbe.place.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import com.hmc.hmcbe.place.entity.SavedPlace;
import com.hmc.hmcbe.place.fixture.SavedPlaceFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgisContainerConfig.class)
class SavedPlaceRepositoryTest {

	@Autowired
	private SavedPlaceRepository savedPlaceRepository;

	@Test
	@DisplayName("사용자의 장소만 저장 순서대로 조회한다")
	void findByUserId() {
		// given
		savedPlaceRepository.save(SavedPlaceFixture.savedPlace(1L, "집"));
		savedPlaceRepository.save(SavedPlaceFixture.savedPlace(2L, "집"));
		savedPlaceRepository.save(SavedPlaceFixture.savedPlace(1L, "회사"));
		savedPlaceRepository.flush();

		// when & then
		assertThat(savedPlaceRepository.findByUserIdOrderByIdAsc(1L))
				.extracting(SavedPlace::getName).containsExactly("집", "회사");
		assertThat(savedPlaceRepository.existsByUserIdAndName(2L, "집")).isTrue();
		assertThat(savedPlaceRepository.existsByUserIdAndName(2L, "회사")).isFalse();
	}

	@Test
	@DisplayName("한 사용자가 같은 이름의 장소를 두 번 저장할 수 없다")
	void uniqueName() {
		savedPlaceRepository.saveAndFlush(SavedPlaceFixture.savedPlace(1L, "집"));

		assertThrows(DataIntegrityViolationException.class,
				() -> savedPlaceRepository.saveAndFlush(SavedPlaceFixture.savedPlace(1L, "집")));
	}
}
