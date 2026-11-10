package com.hmc.hmcbe.user.controller;

import static com.hmc.hmcbe.global.auth.AuthFixture.USER_ID;
import static com.hmc.hmcbe.global.auth.AuthFixture.authenticated;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmc.hmcbe.global.auth.AuthFixture;
import com.hmc.hmcbe.global.exception.BusinessException;
import com.hmc.hmcbe.user.dto.response.UserResponse;
import com.hmc.hmcbe.user.exception.UserErrorCode;
import com.hmc.hmcbe.user.service.UserReadService;
import com.hmc.hmcbe.user.service.UserWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserReadService userReadService;

	@MockitoBean
	private UserWriteService userWriteService;

	@BeforeEach
	void setUp() {
		given(userWriteService.resolveUserId(AuthFixture.SUBJECT)).willReturn(USER_ID);
	}

	@Test
	@DisplayName("내 정보를 조회하면 JWT subject로 찾은 사용자를 반환한다")
	void getMe() throws Exception {
		given(userReadService.getUser(USER_ID)).willReturn(new UserResponse(USER_ID, true));

		mockMvc.perform(get("/api/v1/users/me").with(authenticated()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(1))
				.andExpect(jsonPath("$.data.pushTokenRegistered").value(true));
	}

	@Test
	@DisplayName("JWT 없이 요청하면 401")
	void unauthenticated() throws Exception {
		mockMvc.perform(get("/api/v1/users/me").with(anonymous()))
				.andExpect(status().isUnauthorized());

		verify(userWriteService, never()).resolveUserId(anyString());
	}

	@Test
	@DisplayName("푸시 토큰을 등록한다")
	void updatePushToken() throws Exception {
		mockMvc.perform(put("/api/v1/users/me/push-token").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"pushToken\":\"fcm-token\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		verify(userWriteService).updatePushToken(USER_ID, "fcm-token");
	}

	@Test
	@DisplayName("빈 푸시 토큰은 400 INVALID_REQUEST")
	void updateBlankPushToken() throws Exception {
		mockMvc.perform(put("/api/v1/users/me/push-token").with(authenticated())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"pushToken\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));
	}

	@Test
	@DisplayName("서비스가 NOT_FOUND를 던지면 404로 응답한다")
	void notFound() throws Exception {
		given(userReadService.getUser(USER_ID)).willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

		mockMvc.perform(get("/api/v1/users/me").with(authenticated()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("USER4041"));
	}
}
