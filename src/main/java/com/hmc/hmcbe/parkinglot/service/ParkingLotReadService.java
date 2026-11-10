package com.hmc.hmcbe.parkinglot.service;

import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import java.time.Instant;
import java.util.List;

public interface ParkingLotReadService {

	ParkingLotResponse getParkingLot(Long parkingLotId);

	/** 반경 안의 주차장을 가까운 순으로, 기준 시각(at)이 속한 30분 예측 구간의 만차율과 함께 반환한다. */
	List<NearbyParkingLotResponse> findNearby(GeoPoint center, int radiusMeters, Instant at);

	/** from 이상 to 이하 구간의 예측 만차율을 시각 순으로 반환한다. */
	List<OccupancyForecastResponse> getForecasts(Long parkingLotId, Instant from, Instant to);
}
