package com.hmc.hmcbe.arrival.dto.response;

import com.hmc.hmcbe.arrival.entity.ArrivalSession;
import com.hmc.hmcbe.arrival.entity.ArrivalStatus;
import java.time.Instant;

public record ArrivalSessionResponse(
		Long id,
		String destinationName,
		double latitude,
		double longitude,
		Long destinationParkingLotId,
		Long waypointParkingLotId,
		Instant expectedArrivalAt,
		ArrivalStatus status
) {

	public static ArrivalSessionResponse from(ArrivalSession session) {
		return new ArrivalSessionResponse(
				session.getId(),
				session.getDestinationName(),
				session.getDestinationLocation().latitude(),
				session.getDestinationLocation().longitude(),
				session.getDestinationParkingLotId(),
				session.getWaypointParkingLotId(),
				session.getExpectedArrivalAt(),
				session.getStatus());
	}
}
