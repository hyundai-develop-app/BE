package com.hmc.hmcbe.place.controller.docs;

import com.hmc.hmcbe.global.response.ApiResponse;
import com.hmc.hmcbe.place.dto.request.CreateSavedPlaceRequest;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "SavedPlace", description = "다음 시동 요약의 빠른 목적지(집, 회사 등)")
public interface SavedPlaceControllerDocs {

	@Operation(summary = "내 장소 목록")
	ResponseEntity<ApiResponse<List<SavedPlaceResponse>>> getSavedPlaces(Long userId);

	@Operation(summary = "장소 저장", description = "같은 이름의 장소가 있으면 409 CONFLICT.")
	ResponseEntity<ApiResponse<SavedPlaceResponse>> create(Long userId, CreateSavedPlaceRequest request);

	@Operation(summary = "장소 삭제")
	ResponseEntity<ApiResponse<Void>> delete(Long userId, Long savedPlaceId);
}
