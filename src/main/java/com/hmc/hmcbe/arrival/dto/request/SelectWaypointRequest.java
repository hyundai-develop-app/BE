package com.hmc.hmcbe.arrival.dto.request;

import jakarta.validation.constraints.NotNull;

public record SelectWaypointRequest(@NotNull Long parkingLotId) {
}
