package com.hmc.hmcbe.parking.dto.response;

import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * ⑤ 다음 시동 주차 요약: 경과 시간, 무료 시간, 남은 무료 시간, 빠른 목적지.
 * 주차장을 모르면 parkingLotName·freeMinutes·remainingFreeMinutes는 null이다.
 */
@Schema(description = "다음 시동 주차 요약")
public record ParkingSummaryResponse(
		@Schema(description = "진행 중인 주차 기록")
		ParkingRecordResponse record,
		@Schema(description = "주차장 이름. 주차장을 모르면 null", example = "스타필드 하남 주차장", nullable = true)
		String parkingLotName,
		@Schema(description = "주차 후 경과 시간(분)", example = "95")
		long elapsedMinutes,
		@Schema(description = "주차장 무료 시간(분). 주차장을 모르면 null", example = "180", nullable = true)
		Integer freeMinutes,
		@Schema(description = "남은 무료 시간(분). 0이면 무료 시간 소진. 주차장을 모르면 null", example = "85", nullable = true)
		Long remainingFreeMinutes,
		@Schema(description = "빠른 목적지(저장한 장소, 저장 순). 없으면 빈 배열")
		List<SavedPlaceResponse> quickDestinations
) {
}
