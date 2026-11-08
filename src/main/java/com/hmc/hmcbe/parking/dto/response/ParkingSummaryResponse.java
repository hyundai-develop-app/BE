package com.hmc.hmcbe.parking.dto.response;

import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import java.util.List;

/**
 * ⑤ 다음 시동 주차 요약: 경과 시간, 무료 시간, 남은 무료 시간, 빠른 목적지.
 * 주차장을 모르면 parkingLotName·freeMinutes·remainingFreeMinutes는 null이다.
 */
public record ParkingSummaryResponse(
		ParkingRecordResponse record,
		String parkingLotName,
		long elapsedMinutes,
		Integer freeMinutes,
		Long remainingFreeMinutes,
		List<SavedPlaceResponse> quickDestinations
) {
}
