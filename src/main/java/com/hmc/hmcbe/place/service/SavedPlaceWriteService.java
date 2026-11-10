package com.hmc.hmcbe.place.service;

import com.hmc.hmcbe.place.dto.request.CreateSavedPlaceRequest;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;

public interface SavedPlaceWriteService {

	SavedPlaceResponse create(Long userId, CreateSavedPlaceRequest request);

	void delete(Long userId, Long savedPlaceId);
}
