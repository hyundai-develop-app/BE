package com.hmc.hmcbe.parkinglot.repository;

import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OccupancyForecastRepository extends JpaRepository<OccupancyForecast, Long> {

	Optional<OccupancyForecast> findByParkingLotIdAndTargetAt(Long parkingLotId, Instant targetAt);

	List<OccupancyForecast> findByParkingLotIdAndTargetAtBetweenOrderByTargetAtAsc(Long parkingLotId,
			Instant from, Instant to);

	List<OccupancyForecast> findByParkingLotIdInAndTargetAtBetween(Collection<Long> parkingLotIds,
			Instant from, Instant to);
}
