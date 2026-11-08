package com.hmc.hmcbe.place.service.impl;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.global.geo.GeoPoint;
import com.hmc.hmcbe.place.dto.request.CreateSavedPlaceRequest;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.place.entity.SavedPlace;
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
			throw new BusinessException(ErrorCode.CONFLICT, "같은 이름의 장소가 이미 있습니다");
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
				.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다"));
	}
}
