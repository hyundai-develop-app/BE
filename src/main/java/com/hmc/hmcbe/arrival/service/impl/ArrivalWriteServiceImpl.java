package com.hmc.hmcbe.arrival.service.impl;

import com.hmc.hmcbe.arrival.dto.request.StartArrivalRequest;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;
import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import com.hmc.hmcbe.arrival.repository.ArrivalSessionRepository;
import com.hmc.hmcbe.arrival.service.ArrivalWriteService;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ArrivalWriteServiceImpl implements ArrivalWriteService {

	private final ArrivalSessionRepository arrivalSessionRepository;
	private final ParkingLotReadService parkingLotReadService;
	private final Clock clock;

	@Override
	public ArrivalSessionResponse start(Long userId, StartArrivalRequest request) {
		if (request.destinationParkingLotId() != null) {
			parkingLotReadService.getParkingLot(request.destinationParkingLotId());
		}
		cancelActiveSession(userId);
		ArrivalSession session = ArrivalSession.start(
				userId,
				request.destinationName(),
				new GeoPoint(request.latitude(), request.longitude()),
				request.destinationParkingLotId(),
				request.expectedArrivalAt());
		return ArrivalSessionResponse.from(arrivalSessionRepository.save(session));
	}

	@Override
	public ArrivalSessionResponse selectWaypoint(Long userId, Long sessionId, Long parkingLotId) {
		ArrivalSession session = findOwnedSessionOrThrow(userId, sessionId);
		parkingLotReadService.getParkingLot(parkingLotId);
		session.selectWaypoint(parkingLotId);
		return ArrivalSessionResponse.from(session);
	}

	@Override
	public void complete(Long userId, Long sessionId) {
		findOwnedSessionOrThrow(userId, sessionId).complete(Instant.now(clock));
	}

	@Override
	public void cancel(Long userId, Long sessionId) {
		findOwnedSessionOrThrow(userId, sessionId).cancel(Instant.now(clock));
	}

	/**
	 * Hibernate는 flush 때 INSERT를 UPDATE보다 먼저 실행하므로, 기존 세션 취소를 먼저 flush해야
	 * "사용자당 ACTIVE 하나" 부분 유니크 인덱스에 걸리지 않는다.
	 */
	private void cancelActiveSession(Long userId) {
		arrivalSessionRepository.findByUserIdAndStatus(userId, ArrivalStatus.ACTIVE)
				.ifPresent(active -> {
					active.cancel(Instant.now(clock));
					arrivalSessionRepository.flush();
				});
	}

	/** 다른 사용자의 세션은 존재 여부를 드러내지 않도록 NOT_FOUND로 응답한다. */
	private ArrivalSession findOwnedSessionOrThrow(Long userId, Long sessionId) {
		return arrivalSessionRepository.findById(sessionId)
				.filter(session -> session.isOwnedBy(userId))
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "도착 모드를 찾을 수 없습니다"));
	}
}
