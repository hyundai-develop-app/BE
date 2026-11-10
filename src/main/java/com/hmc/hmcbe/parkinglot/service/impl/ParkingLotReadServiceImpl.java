package com.hmc.hmcbe.parkinglot.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.parkinglot.dto.response.NearbyParkingLotResponse;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.dto.response.ParkingLotResponse;
import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import com.hmc.hmcbe.parkinglot.entity.ParkingLot;
import com.hmc.hmcbe.parkinglot.exception.ParkingLotErrorCode;
import com.hmc.hmcbe.parkinglot.repository.NearbyParkingLotProjection;
import com.hmc.hmcbe.parkinglot.repository.OccupancyForecastRepository;
import com.hmc.hmcbe.parkinglot.repository.ParkingLotRepository;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParkingLotReadServiceImpl implements ParkingLotReadService {

	static final int MAX_RADIUS_METERS = 5_000;
	static final int MAX_NEARBY_RESULTS = 10;
	/** 예측은 30분 간격 슬롯으로 저장된다. 기준 시각이 속한 슬롯 = (at - 30분, at] 안의 가장 늦은 예측. */
	static final Duration FORECAST_SLOT = Duration.ofMinutes(30);

	private final ParkingLotRepository parkingLotRepository;
	private final OccupancyForecastRepository occupancyForecastRepository;

	@Override
	public ParkingLotResponse getParkingLot(Long parkingLotId) {
		return ParkingLotResponse.from(findParkingLotOrThrow(parkingLotId));
	}

	@Override
	public List<NearbyParkingLotResponse> findNearby(GeoPoint center, int radiusMeters, Instant at) {
		if (radiusMeters < 1 || radiusMeters > MAX_RADIUS_METERS) {
			throw new BusinessException(ParkingLotErrorCode.INVALID_SEARCH_RADIUS,
					"검색 반경은 1 ~ " + MAX_RADIUS_METERS + "m 사이여야 합니다");
		}
		List<NearbyParkingLotProjection> nearby = parkingLotRepository.findNearby(
				center.latitude(), center.longitude(), radiusMeters, MAX_NEARBY_RESULTS);
		if (nearby.isEmpty()) {
			return List.of();
		}
		List<Long> ids = nearby.stream().map(NearbyParkingLotProjection::getId).toList();
		Map<Long, ParkingLot> lots = parkingLotRepository.findAllById(ids).stream()
				.collect(Collectors.toMap(ParkingLot::getId, Function.identity()));
		Map<Long, OccupancyForecast> forecasts = latestForecastsInSlot(ids, at);

		return nearby.stream()
				.filter(p -> lots.containsKey(p.getId()))
				.map(p -> NearbyParkingLotResponse.of(
						ParkingLotResponse.from(lots.get(p.getId())),
						p.getDistanceMeters(),
						forecasts.containsKey(p.getId())
								? OccupancyForecastResponse.from(forecasts.get(p.getId()))
								: null))
				.toList();
	}

	@Override
	public List<OccupancyForecastResponse> getForecasts(Long parkingLotId, Instant from, Instant to) {
		if (from.isAfter(to)) {
			throw new BusinessException(ParkingLotErrorCode.INVALID_FORECAST_PERIOD, "조회 시작 시각이 끝 시각보다 늦습니다");
		}
		findParkingLotOrThrow(parkingLotId);
		return occupancyForecastRepository
				.findByParkingLotIdAndTargetAtBetweenOrderByTargetAtAsc(parkingLotId, from, to).stream()
				.map(OccupancyForecastResponse::from)
				.toList();
	}

	private Map<Long, OccupancyForecast> latestForecastsInSlot(List<Long> parkingLotIds, Instant at) {
		Instant slotStart = at.minus(FORECAST_SLOT).plusNanos(1);
		return occupancyForecastRepository.findByParkingLotIdInAndTargetAtBetween(parkingLotIds, slotStart, at)
				.stream()
				.collect(Collectors.toMap(OccupancyForecast::getParkingLotId, Function.identity(),
						BinaryOperator.maxBy(Comparator.comparing(OccupancyForecast::getTargetAt))));
	}

	private ParkingLot findParkingLotOrThrow(Long parkingLotId) {
		return parkingLotRepository.findById(parkingLotId)
				.orElseThrow(() -> new BusinessException(ParkingLotErrorCode.PARKING_LOT_NOT_FOUND));
	}
}
