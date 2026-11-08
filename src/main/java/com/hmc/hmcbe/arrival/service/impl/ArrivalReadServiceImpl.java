package com.hmc.hmcbe.arrival.service.impl;

import com.hmc.hmcbe.arrival.dto.response.ArrivalOutlookResponse;
import com.hmc.hmcbe.arrival.dto.response.ArrivalSessionResponse;
import com.hmc.hmcbe.arrival.dto.response.ParkingBriefingResponse;
import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import com.hmc.hmcbe.arrival.repository.ArrivalSessionRepository;
import com.hmc.hmcbe.arrival.service.ArrivalReadService;
import com.hmc.hmcbe.arrival.service.ParkingRecommendationPolicy;
import com.hmc.hmcbe.arrival.service.ParkingRecommendationPolicy.Recommendation;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.parkinglot.dto.response.OccupancyForecastResponse;
import com.hmc.hmcbe.parkinglot.service.ParkingLotReadService;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArrivalReadServiceImpl implements ArrivalReadService {

	/** 목적지 주변 대안 주차장 검색 반경. 화면 예시("300m 옆 공영주차장")를 포함하는 도보 거리로 잡았다. */
	static final int SEARCH_RADIUS_METERS = 500;
	/** ① 막대그래프 범위: 도착 시각 전후 1시간. */
	static final Duration FORECAST_WINDOW = Duration.ofHours(1);

	private final ArrivalSessionRepository arrivalSessionRepository;
	private final ParkingLotReadService parkingLotReadService;
	private final ParkingRecommendationPolicy recommendationPolicy;

	@Override
	public ArrivalSessionResponse getActiveSession(Long userId) {
		return arrivalSessionRepository.findByUserIdAndStatus(userId, ArrivalStatus.ACTIVE)
				.map(ArrivalSessionResponse::from)
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "진행 중인 도착 모드가 없습니다"));
	}

	@Override
	public ArrivalOutlookResponse getOutlook(Long userId, Long sessionId) {
		ArrivalSession session = findOwnedSessionOrThrow(userId, sessionId);
		Recommendation recommendation = recommend(session);
		return new ArrivalOutlookResponse(
				ArrivalSessionResponse.from(session),
				recommendation.destination(),
				destinationForecasts(session),
				recommendation.alternatives().size());
	}

	@Override
	public ParkingBriefingResponse getBriefing(Long userId, Long sessionId) {
		Recommendation recommendation = recommend(findOwnedSessionOrThrow(userId, sessionId));
		return new ParkingBriefingResponse(
				recommendation.destination(),
				recommendation.recommended(),
				recommendation.alternatives());
	}

	private Recommendation recommend(ArrivalSession session) {
		return recommendationPolicy.recommend(
				session.getDestinationParkingLotId(),
				parkingLotReadService.findNearby(session.getDestinationLocation(), SEARCH_RADIUS_METERS,
						session.getExpectedArrivalAt()));
	}

	private List<OccupancyForecastResponse> destinationForecasts(ArrivalSession session) {
		if (session.getDestinationParkingLotId() == null) {
			return List.of();
		}
		return parkingLotReadService.getForecasts(
				session.getDestinationParkingLotId(),
				session.getExpectedArrivalAt().minus(FORECAST_WINDOW),
				session.getExpectedArrivalAt().plus(FORECAST_WINDOW));
	}

	/** 다른 사용자의 세션은 존재 여부를 드러내지 않도록 NOT_FOUND로 응답한다. */
	private ArrivalSession findOwnedSessionOrThrow(Long userId, Long sessionId) {
		return arrivalSessionRepository.findById(sessionId)
				.filter(session -> session.isOwnedBy(userId))
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "도착 모드를 찾을 수 없습니다"));
	}
}
