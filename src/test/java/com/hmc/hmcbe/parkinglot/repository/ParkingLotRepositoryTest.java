package com.hmc.hmcbe.parkinglot.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import com.hmc.hmcbe.parkinglot.entity.ParkingFee;
import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import com.hmc.hmcbe.parkinglot.fixture.ParkingLotFixture;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgisContainerConfig.class)
class ParkingLotRepositoryTest {

	@Autowired
	private ParkingLotRepository parkingLotRepository;

	@Autowired
	private OccupancyForecastRepository occupancyForecastRepository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("반경 안의 주차장만 가까운 순으로 미터 거리와 함께 조회한다")
	void findNearby() {
		// given — 위도 0.001도 ≈ 111m
		GeoPoint center = ParkingLotFixture.YEOUIDO;
		ParkingLot near = parkingLotRepository.save(ParkingLotFixture.parkingLot("가까운", offset(center, 0.001)));
		ParkingLot middle = parkingLotRepository.save(ParkingLotFixture.parkingLot("중간", offset(center, 0.003)));
		parkingLotRepository.save(ParkingLotFixture.parkingLot("먼", offset(center, 0.01)));
		parkingLotRepository.flush();

		// when
		List<NearbyParkingLotProjection> result =
				parkingLotRepository.findNearby(center.latitude(), center.longitude(), 500, 10);

		// then
		assertThat(result).extracting(NearbyParkingLotProjection::getId)
				.containsExactly(near.getId(), middle.getId());
		assertThat(result.get(0).getDistanceMeters()).isCloseTo(111, within(2.0));
	}

	@Test
	@DisplayName("위치와 요금(Embeddable)이 그대로 저장·조회된다")
	void persistLocationAndFee() {
		// given
		ParkingLot saved = parkingLotRepository.save(ParkingLotFixture.parkingLot());
		ParkingLot free = parkingLotRepository.save(
				ParkingLot.create("무료", ParkingLotFixture.YEOUIDO, false, null, 0, null, null));
		entityManager.flush();
		entityManager.clear();

		// when
		ParkingLot found = parkingLotRepository.findById(saved.getId()).orElseThrow();
		ParkingLot foundFree = parkingLotRepository.findById(free.getId()).orElseThrow();

		// then
		assertThat(found.getLocation()).isEqualTo(ParkingLotFixture.YEOUIDO);
		assertThat(found.getFee()).isEqualTo(new ParkingFee(10, 1000));
		assertThat(foundFree.getFee()).isNull();
	}

	@Test
	@DisplayName("예측을 주차장 목록과 시각 구간으로 조회한다")
	void findForecasts() {
		// given
		Long lotId = parkingLotRepository.save(ParkingLotFixture.parkingLot()).getId();
		Instant t1 = Instant.parse("2026-11-09T05:00:00Z");
		Instant t2 = Instant.parse("2026-11-09T05:30:00Z");
		Instant t3 = Instant.parse("2026-11-09T06:00:00Z");
		occupancyForecastRepository.saveAll(List.of(
				OccupancyForecast.create(lotId, t3, 82),
				OccupancyForecast.create(lotId, t1, 58),
				OccupancyForecast.create(lotId, t2, 95)));
		occupancyForecastRepository.flush();

		// when & then
		assertThat(occupancyForecastRepository.findByParkingLotIdAndTargetAtBetweenOrderByTargetAtAsc(lotId, t1, t2))
				.extracting(OccupancyForecast::getOccupancyRate).containsExactly(58, 95);
		assertThat(occupancyForecastRepository.findByParkingLotIdInAndTargetAtBetween(List.of(lotId), t2, t3))
				.hasSize(2);
		assertThat(occupancyForecastRepository.findByParkingLotIdAndTargetAt(lotId, t2)).isPresent();
	}

	private static GeoPoint offset(GeoPoint base, double latitudeDelta) {
		return new GeoPoint(base.latitude() + latitudeDelta, base.longitude());
	}
}
