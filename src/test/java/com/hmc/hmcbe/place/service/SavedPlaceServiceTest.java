package com.hmc.hmcbe.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.global.exception.ErrorCode;
import com.hmc.hmcbe.place.dto.request.CreateSavedPlaceRequest;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.place.entity.SavedPlace;
import com.hmc.hmcbe.place.fixture.SavedPlaceFixture;
import com.hmc.hmcbe.place.repository.SavedPlaceRepository;
import com.hmc.hmcbe.place.service.impl.SavedPlaceReadServiceImpl;
import com.hmc.hmcbe.place.service.impl.SavedPlaceWriteServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavedPlaceServiceTest {

	@Mock
	private SavedPlaceRepository savedPlaceRepository;

	@InjectMocks
	private SavedPlaceWriteServiceImpl savedPlaceWriteService;

	@InjectMocks
	private SavedPlaceReadServiceImpl savedPlaceReadService;

	@Test
	@DisplayName("장소를 저장하면 저장된 장소를 반환한다")
	void create() {
		// given
		given(savedPlaceRepository.existsByUserIdAndName(1L, "집")).willReturn(false);
		given(savedPlaceRepository.save(any(SavedPlace.class))).willAnswer(invocation -> invocation.getArgument(0));

		// when
		SavedPlaceResponse response = savedPlaceWriteService.create(1L,
				new CreateSavedPlaceRequest("집", 37.4979, 127.0276));

		// then
		assertThat(response.name()).isEqualTo("집");
		assertThat(response.latitude()).isEqualTo(37.4979);
	}

	@Test
	@DisplayName("같은 이름의 장소가 이미 있으면 CONFLICT")
	void createDuplicate() {
		given(savedPlaceRepository.existsByUserIdAndName(1L, "집")).willReturn(true);

		BusinessException ex = assertThrows(BusinessException.class, () -> savedPlaceWriteService.create(1L,
				new CreateSavedPlaceRequest("집", 37.4979, 127.0276)));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
		verify(savedPlaceRepository, never()).save(any());
	}

	@Test
	@DisplayName("본인 장소를 삭제한다")
	void delete() {
		SavedPlace place = SavedPlaceFixture.savedPlaceWithId(10L, 1L);
		given(savedPlaceRepository.findById(10L)).willReturn(Optional.of(place));

		savedPlaceWriteService.delete(1L, 10L);

		verify(savedPlaceRepository).delete(place);
	}

	@Test
	@DisplayName("다른 사용자의 장소를 삭제하면 NOT_FOUND")
	void deleteOthers() {
		given(savedPlaceRepository.findById(10L)).willReturn(Optional.of(SavedPlaceFixture.savedPlaceWithId(10L, 2L)));

		BusinessException ex = assertThrows(BusinessException.class, () -> savedPlaceWriteService.delete(1L, 10L));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
		verify(savedPlaceRepository, never()).delete(any());
	}

	@Test
	@DisplayName("사용자의 장소 목록을 조회한다")
	void getSavedPlaces() {
		given(savedPlaceRepository.findByUserIdOrderByIdAsc(1L)).willReturn(List.of(
				SavedPlaceFixture.savedPlace(1L, "집"), SavedPlaceFixture.savedPlace(1L, "회사")));

		assertThat(savedPlaceReadService.getSavedPlaces(1L))
				.extracting(SavedPlaceResponse::name).containsExactly("집", "회사");
	}
}
