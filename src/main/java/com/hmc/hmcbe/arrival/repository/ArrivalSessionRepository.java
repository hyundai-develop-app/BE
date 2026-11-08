package com.hmc.hmcbe.arrival.repository;

import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArrivalSessionRepository extends JpaRepository<ArrivalSession, Long> {

	Optional<ArrivalSession> findByUserIdAndStatus(Long userId, ArrivalStatus status);
}
