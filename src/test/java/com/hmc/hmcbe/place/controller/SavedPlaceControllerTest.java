package com.hmc.hmcbe.place.controller;

import static com.hmc.hmcbe.global.auth.AuthFixture.USER_ID;
import static com.hmc.hmcbe.global.auth.AuthFixture.authenticated;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmc.hmcbe.global.auth.AuthFixture;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.place.dto.request.CreateSavedPlaceRequest;
import com.hmc.hmcbe.place.dto.response.SavedPlaceResponse;
import com.hmc.hmcbe.place.exception.PlaceErrorCode;
import com.hmc.hmcbe.place.service.SavedPlaceReadService;
import com.hmc.hmcbe.place.service.SavedPlaceWriteService;
import com.hmc.hmcbe.user.service.UserWriteService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SavedPlaceController.class)
class SavedPlaceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SavedPlaceReadService savedPlaceReadService;

	@MockitoBean
	private SavedPlaceWriteService savedPlaceWriteService;

	@MockitoBean
	private UserWriteService userWriteService;

	@BeforeEach
	void setUp() {
		given(userWriteService.resolveUserId(AuthFixture.SUBJECT)).willReturn(USER_ID);
	}

	@Test
	@DisplayName("내 장소 목록을 조회한다")
	void getSavedPlaces() throws Exception {
		given(savedPlaceReadService.getSavedPlaces(USER_ID)).willReturn(List.of(
				new SavedPlaceResponse(1L, "집", 37.4979, 127.0276)));

		mockMvc.perform(get("/api/v1/saved-places").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].name").value("집"));
	}

	@Test
	@DisplayName("장소를 저장하면 201로 저장된 장소를 반환한다")
	void create() throws Exception {
		CreateSavedPlaceRequest request = new CreateSavedPlaceRequest("회사", 37.51, 127.05);
		given(savedPlaceWriteService.create(USER_ID, request))
				.willReturn(new SavedPlaceResponse(2L, "회사", 37.51, 127.05));

		mockMvc.perform(post("/api/v1/saved-places").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"회사\",\"latitude\":37.51,\"longitude\":127.05}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.id").value(2));
	}

	@Test
	@DisplayName("이름이 없으면 400 INVALID_REQUEST")
	void createWithoutName() throws Exception {
		mockMvc.perform(post("/api/v1/saved-places").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"latitude\":37.51,\"longitude\":127.05}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));

		verifyNoInteractions(savedPlaceWriteService);
	}

	@Test
	@DisplayName("같은 이름이 있으면 409 CONFLICT")
	void createDuplicate() throws Exception {
		CreateSavedPlaceRequest request = new CreateSavedPlaceRequest("집", 37.51, 127.05);
		given(savedPlaceWriteService.create(USER_ID, request))
				.willThrow(new BusinessException(PlaceErrorCode.DUPLICATE_PLACE_NAME));

		mockMvc.perform(post("/api/v1/saved-places").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"집\",\"latitude\":37.51,\"longitude\":127.05}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("PLACE4091"));
	}

	@Test
	@DisplayName("장소를 삭제한다")
	void deletePlace() throws Exception {
		mockMvc.perform(delete("/api/v1/saved-places/10").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		verify(savedPlaceWriteService).delete(USER_ID, 10L);
	}
}
