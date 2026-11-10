package com.hmc.hmcbe.arrival.service;

import com.hmc.hmcbe.arrival.dto.request.StartArrivalRequest;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;

public interface ArrivalWriteService {

	/** 도착 모드를 시작한다. 이미 진행 중인 세션이 있으면 취소하고 새로 시작한다(목적지 변경). */
	ArrivalSessionResponse start(Long userId, StartArrivalRequest request);

	ArrivalSessionResponse selectWaypoint(Long userId, Long sessionId, Long parkingLotId);

	void complete(Long userId, Long sessionId);

	void cancel(Long userId, Long sessionId);
}
