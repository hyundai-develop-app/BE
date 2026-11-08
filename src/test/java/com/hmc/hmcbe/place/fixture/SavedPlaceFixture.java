package com.hmc.hmcbe.place.fixture;

import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.place.entity.SavedPlace;
import org.springframework.test.util.ReflectionTestUtils;

public final class SavedPlaceFixture {

	public static final GeoPoint HOME = new GeoPoint(37.4979, 127.0276);

	private SavedPlaceFixture() {
	}

	public static SavedPlace savedPlace(Long userId, String name) {
		return SavedPlace.create(userId, name, HOME);
	}

	public static SavedPlace savedPlaceWithId(Long id, Long userId) {
		SavedPlace place = savedPlace(userId, "집");
		ReflectionTestUtils.setField(place, "id", id);
		return place;
	}
}
