package com.hmc.hmcbe.parkinglot.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.dto.request.RegisterParkingLotRequest;
import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import com.hmc.hmcbe.parkinglot.entity.ParkingFee;
import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import com.hmc.hmcbe.parkinglot.repository.OccupancyForecastRepository;
import com.hmc.hmcbe.parkinglot.repository.ParkingLotRepository;
import com.hmc.hmcbe.parkinglot.service.ParkingLotWriteService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ParkingLotWriteServiceImpl implements ParkingLotWriteService {

	private final ParkingLotRepository parkingLotRepository;
	private final OccupancyForecastRepository occupancyForecastRepository;

	@Override
	public Long register(RegisterParkingLotRequest request) {
		ParkingLot lot = ParkingLot.create(
				request.name(),
				new GeoPoint(request.latitude(), request.longitude()),
				request.indoor(),
				request.entranceGuide(),
				request.freeMinutes(),
				toFee(request.feeUnitMinutes(), request.feeUnitPrice()),
				request.totalSpaces(),
				request.address(),
				request.feeDescription(),
				Boolean.TRUE.equals(request.weekendOnly()));
		return parkingLotRepository.save(lot).getId();
	}

	@Override
	public void recordForecast(Long parkingLotId, Instant targetAt, int occupancyRate) {
		if (!parkingLotRepository.existsById(parkingLotId)) {
			throw new BusinessException(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND);
		}
		occupancyForecastRepository.findByParkingLotIdAndTargetAt(parkingLotId, targetAt)
				.ifPresentOrElse(
						forecast -> forecast.updateOccupancyRate(occupancyRate),
						() -> occupancyForecastRepository.save(
								OccupancyForecast.create(parkingLotId, targetAt, occupancyRate)));
	}

	private ParkingFee toFee(Integer unitMinutes, Integer unitPrice) {
		if (unitMinutes == null && unitPrice == null) {
			return null;
		}
		if (unitMinutes == null || unitPrice == null) {
			throw new BusinessException(ParkingLotErrorCode.INVALID_FEE, "요금 단위 시간과 단위 요금은 함께 입력해야 합니다");
		}
		return new ParkingFee(unitMinutes, unitPrice);
	}
}
