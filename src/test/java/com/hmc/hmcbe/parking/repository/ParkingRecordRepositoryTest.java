package com.hmc.hmcbe.parking.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import com.hmc.hmcbe.parking.entity.ParkingRecord;
import com.hmc.hmcbe.parking.fixture.ParkingRecordFixture;
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
class ParkingRecordRepositoryTest {

	@Autowired
	private ParkingRecordRepository parkingRecordRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("출차하지 않은 기록만 진행 중으로 찾는다")
	void findActive() {
		// given
		ParkingRecord exited = parkingRecordRepository.saveAndFlush(ParkingRecordFixture.activeRecord(1L));
		exited.exit(ParkingRecordFixture.PARKED_AT.plusSeconds(3600));
		parkingRecordRepository.flush();
		ParkingRecord active = parkingRecordRepository.saveAndFlush(ParkingRecordFixture.activeRecord(1L, 7L));
		active.updateSpotMemo("지하 2층 C구역");
		parkingRecordRepository.flush();
		entityManager.clear();

		// when
		ParkingRecord found = parkingRecordRepository.findByUserIdAndExitedAtIsNull(1L).orElseThrow();

		// then
		assertThat(found.getId()).isEqualTo(active.getId());
		assertThat(found.getSpotMemo()).isEqualTo("지하 2층 C구역");
		assertThat(found.getLocation()).isEqualTo(ParkingRecordFixture.LOCATION);
		assertThat(parkingRecordRepository.findByUserIdAndExitedAtIsNull(2L)).isEmpty();
	}

	@Test
	@DisplayName("한 사용자가 출차하지 않은 기록을 둘 가질 수 없다")
	void singleActivePerUser() {
		parkingRecordRepository.saveAndFlush(ParkingRecordFixture.activeRecord(1L));

		assertThrows(DataIntegrityViolationException.class,
				() -> parkingRecordRepository.saveAndFlush(ParkingRecordFixture.activeRecord(1L)));
	}
}
