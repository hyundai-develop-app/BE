package com.hmc.hmcbe.parking.service;

import com.hmc.hmcbe.parking.dto.response.ParkingRecordResponse;
import com.hmc.hmcbe.parking.dto.response.ParkingSummaryResponse;

public interface ParkingRecordReadService {

	ParkingRecordResponse getActiveRecord(Long userId);

	/** 출차하지 않은 주차 기록의 요약(⑤ 다음 시동 화면). */
	ParkingSummaryResponse getSummary(Long userId);
}
