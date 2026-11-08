package com.hmc.hmcbe.place.service.impl;

import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.place.repository.SavedPlaceRepository;
import com.hmc.hmcbe.place.service.SavedPlaceReadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedPlaceReadServiceImpl implements SavedPlaceReadService {

	private final SavedPlaceRepository savedPlaceRepository;

	@Override
	public List<SavedPlaceResponse> getSavedPlaces(Long userId) {
		return savedPlaceRepository.findByUserIdOrderByIdAsc(userId).stream()
				.map(SavedPlaceResponse::from)
				.toList();
	}
}
