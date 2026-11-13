package com.hmc.hmcbe.parkinglot.dto.response;

import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** congestionLevel은 AVAILABLE(여유) / NORMAL(보통) / CONGESTED(혼잡) 중 하나다. */
@Schema(description = "예측 만차율")
public record OccupancyForecastResponse(
		@Schema(description = "예측 대상 시각(30분 간격 슬롯)", example = "2026-11-14T09:30:00Z")
		Instant targetAt,
		@Schema(description = "예측 만차율(%, 0~100)", example = "45", minimum = "0", maximum = "100")
		int occupancyRate,
		@Schema(description = "혼잡도. AVAILABLE(여유, 70% 미만) / NORMAL(보통, 70~89%) / CONGESTED(혼잡, 90% 이상)",
				example = "AVAILABLE", allowableValues = {"AVAILABLE", "NORMAL", "CONGESTED"})
		String congestionLevel) {
	public static OccupancyForecastResponse from(OccupancyForecast forecast) {
		return new OccupancyForecastResponse(
				forecast.getTargetAt(),
				forecast.getOccupancyRate(),
				forecast.congestionLevel().name());
	}
}
