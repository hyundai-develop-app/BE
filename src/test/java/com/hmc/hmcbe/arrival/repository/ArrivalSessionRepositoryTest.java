package com.hmc.hmcbe.arrival.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import com.hmc.hmcbe.arrival.fixture.ArrivalSessionFixture;
import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgisContainerConfig.class)
class ArrivalSessionRepositoryTest {

	private static final Instant NOW = Instant.parse("2026-11-09T05:00:00Z");

	@Autowired
	private ArrivalSessionRepository arrivalSessionRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("사용자의 ACTIVE 세션을 찾고, 끝난 세션은 찾지 않는다")
	void findActive() {
		// given
		ArrivalSession ended = arrivalSessionRepository.saveAndFlush(ArrivalSessionFixture.activeSession(1L));
		ended.complete(NOW);
		arrivalSessionRepository.flush();
		ArrivalSession active = arrivalSessionRepository.saveAndFlush(ArrivalSessionFixture.activeSession(1L, 7L));
		entityManager.clear();

		// when
		ArrivalSession found = arrivalSessionRepository.findByUserIdAndStatus(1L, ArrivalStatus.ACTIVE).orElseThrow();

		// then
		assertThat(found.getId()).isEqualTo(active.getId());
		assertThat(found.getDestinationLocation()).isEqualTo(ArrivalSessionFixture.DESTINATION);
		assertThat(found.getExpectedArrivalAt()).isEqualTo(ArrivalSessionFixture.ETA);
		assertThat(arrivalSessionRepository.findByUserIdAndStatus(2L, ArrivalStatus.ACTIVE)).isEmpty();
	}

	@Test
	@DisplayName("한 사용자가 ACTIVE 세션을 둘 가질 수 없다")
	void singleActivePerUser() {
		arrivalSessionRepository.saveAndFlush(ArrivalSessionFixture.activeSession(1L));

		assertThrows(DataIntegrityViolationException.class,
				() -> arrivalSessionRepository.saveAndFlush(ArrivalSessionFixture.activeSession(1L)));
	}
}
