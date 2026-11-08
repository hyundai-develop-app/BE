package com.hmc.hmcbe.parkinglot.dto.response;

import com.hmc.hmcbe.parkinglot.entity.OccupancyForecast;
import java.time.Instant;

/** congestionLevel은 AVAILABLE(여유) / NORMAL(보통) / CONGESTED(혼잡) 중 하나다. */
public record OccupancyForecastResponse(Instant targetAt, int occupancyRate, String congestionLevel) {

	public static OccupancyForecastResponse from(OccupancyForecast forecast) {
		return new OccupancyForecastResponse(
				forecast.getTargetAt(),
				forecast.getOccupancyRate(),
				forecast.congestionLevel().name());
	}
}
