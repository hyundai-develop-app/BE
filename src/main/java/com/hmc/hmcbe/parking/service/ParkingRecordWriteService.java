package com.hmc.hmcbe.parking.service;

import com.hmc.hmcbe.parking.dto.request.ParkRequest;
import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;

public interface ParkingRecordWriteService {

	/** P단 주차를 기록한다. 출차하지 않은 이전 기록이 있으면 지금 시각으로 출차 처리한 뒤 새로 기록한다. */
	ParkingRecordResponse park(Long userId, ParkRequest request);

	ParkingRecordResponse updateSpotMemo(Long userId, Long recordId, String spotMemo);

	void exit(Long userId, Long recordId);
}
