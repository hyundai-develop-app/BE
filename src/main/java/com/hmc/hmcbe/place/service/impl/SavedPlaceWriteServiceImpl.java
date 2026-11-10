package com.hmc.hmcbe.place.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.place.dto.request.CreateSavedPlaceRequest;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.place.entity.SavedPlace;
import com.hmc.hmcbe.place.exception.PlaceErrorCode;
import com.hmc.hmcbe.place.repository.SavedPlaceRepository;
import com.hmc.hmcbe.place.service.SavedPlaceWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SavedPlaceWriteServiceImpl implements SavedPlaceWriteService {

	private final SavedPlaceRepository savedPlaceRepository;

	@Override
	public SavedPlaceResponse create(Long userId, CreateSavedPlaceRequest request) {
		if (savedPlaceRepository.existsByUserIdAndName(userId, request.name())) {
			throw new BusinessException(PlaceErrorCode.DUPLICATE_PLACE_NAME);
		}
		SavedPlace place = SavedPlace.create(userId, request.name(),
				new GeoPoint(request.latitude(), request.longitude()));
		return SavedPlaceResponse.from(savedPlaceRepository.save(place));
	}

	@Override
	public void delete(Long userId, Long savedPlaceId) {
		savedPlaceRepository.delete(findOwnedPlaceOrThrow(userId, savedPlaceId));
	}

	/** 다른 사용자의 장소는 존재 여부를 드러내지 않도록 NOT_FOUND로 응답한다. */
	private SavedPlace findOwnedPlaceOrThrow(Long userId, Long savedPlaceId) {
		return savedPlaceRepository.findById(savedPlaceId)
				.filter(place -> place.isOwnedBy(userId))
				.orElseThrow(() -> new BusinessException(PlaceErrorCode.SAVED_PLACE_NOT_FOUND));
	}
}
