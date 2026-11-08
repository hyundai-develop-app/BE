package com.hmc.hmcbe.place.dto.response;

import com.hmc.hmcbe.place.entity.SavedPlace;

public record SavedPlaceResponse(Long id, String name, double latitude, double longitude) {

	public static SavedPlaceResponse from(SavedPlace place) {
		return new SavedPlaceResponse(
				place.getId(),
				place.getName(),
				place.getLocation().latitude(),
				place.getLocation().longitude());
	}
}
