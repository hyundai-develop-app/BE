package com.hmc.hmcbe.place.dto.response;

import com.hmc.hmcbe.place.entity.SavedPlace;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "저장한 장소")
public record SavedPlaceResponse(
		@Schema(description = "장소 ID", example = "3")
		Long id,
		@Schema(description = "장소 이름", example = "집")
		String name,
		@Schema(description = "위도", example = "37.4979")
		double latitude,
		@Schema(description = "경도", example = "127.0276")
		double longitude) {
	public static SavedPlaceResponse from(SavedPlace place) {
		return new SavedPlaceResponse(
				place.getId(),
				place.getName(),
				place.getLocation().latitude(),
				place.getLocation().longitude());
	}
}
