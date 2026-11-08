package com.hmc.hmcbe.parkinglot.service;

import com.hmc.hmcbe.parkinglot.dto.request.RegisterParkingLotRequest;
import java.time.Instant;

public interface ParkingLotWriteService {

	Long register(RegisterParkingLotRequest request);

	/** 같은 주차장·시각의 예측이 있으면 만차율을 교체하고, 없으면 새로 저장한다. */
	void recordForecast(Long parkingLotId, Instant targetAt, int occupancyRate);
}
