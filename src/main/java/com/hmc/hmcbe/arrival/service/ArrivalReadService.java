package com.hmc.hmcbe.arrival.service;

import com.hmc.hmcbe.arrival.dto.response.ArrivalOutlookResponse;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;
import com.hmc.hmcbe.arrival.dto.response.ParkingBriefingResponse;

public interface ArrivalReadService {

	ArrivalSessionResponse getActiveSession(Long userId);

	/** ① 주행 중 대기 화면 데이터. */
	ArrivalOutlookResponse getOutlook(Long userId, Long sessionId);

	/** ② 도착 10분 전 주차 브리핑 데이터. */
	ParkingBriefingResponse getBriefing(Long userId, Long sessionId);
}
