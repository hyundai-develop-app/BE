package com.hmc.hmcbe.parking.repository;

import com.hmc.hmcbe.parking.entity.ParkingRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParkingRecordRepository extends JpaRepository<ParkingRecord, Long> {

	Optional<ParkingRecord> findByUserIdAndExitedAtIsNull(Long userId);
}
