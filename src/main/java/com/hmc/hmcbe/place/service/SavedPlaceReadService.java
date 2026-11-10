package com.hmc.hmcbe.place.service;

import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import java.util.List;

public interface SavedPlaceReadService {

	List<SavedPlaceResponse> getSavedPlaces(Long userId);
}
