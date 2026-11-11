package com.hmc.hmcbe.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new StubController())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	@DisplayName("BusinessException이 발생하면 ErrorCode의 상태·코드·메시지로 실패 응답한다")
	void business() throws Exception {
		mockMvc.perform(get("/business"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").value("COMMON4041"));
	}

	@Test
	@DisplayName("요청 바디 검증에 실패하면 400 COMMON4001로 응답한다")
	void validation() throws Exception {
		mockMvc.perform(post("/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));
	}

	@Test
	@DisplayName("요청 바디를 읽을 수 없으면 400 COMMON4001로 응답한다")
	void malformedBody() throws Exception {
		mockMvc.perform(post("/validate")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));
	}

	@Test
	@DisplayName("필수 요청 파라미터가 없으면 400 COMMON4001로 응답한다")
	void missingParam() throws Exception {
		mockMvc.perform(get("/param"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));
	}

	@Test
	@DisplayName("요청 파라미터 타입이 맞지 않으면 400 COMMON4001로 응답한다")
	void typeMismatch() throws Exception {
		mockMvc.perform(get("/param").param("id", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("COMMON4001"));
	}

	@Test
	@DisplayName("인증 예외가 발생하면 401 COMMON4011로 응답한다")
	void authentication() throws Exception {
		mockMvc.perform(get("/unauthenticated"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("COMMON4011"));
	}

	@Test
	@DisplayName("인가 예외가 발생하면 403 COMMON4031으로 응답한다")
	void accessDenied() throws Exception {
		mockMvc.perform(get("/forbidden"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error.code").value("COMMON4031"));
	}

	@Test
	@DisplayName("지원하지 않는 HTTP 메서드로 요청하면 405 COMMON4051로 응답한다")
	void methodNotSupported() throws Exception {
		mockMvc.perform(post("/business"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.error.code").value("COMMON4051"));
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type으로 요청하면 415 COMMON4151로 응답한다")
	void mediaTypeNotSupported() throws Exception {
		mockMvc.perform(post("/validate")
						.contentType(MediaType.TEXT_PLAIN)
						.content("name"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error.code").value("COMMON4151"));
	}

	@Test
	@DisplayName("DB 제약조건 경합이 발생하면 내부 메시지를 숨기고 409 COMMON4091로 응답한다")
	void dataIntegrityConflict() throws Exception {
		mockMvc.perform(get("/integrity-conflict"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.data").doesNotExist())
				.andExpect(jsonPath("$.error.code").value("COMMON4091"))
				.andExpect(jsonPath("$.error.message").value(CommonErrorCode.CONFLICT.getDefaultMessage()));
	}

	@Test
	@DisplayName("처리되지 않은 예외가 발생하면 500 COMMON5001로 응답하고 내부 메시지를 노출하지 않는다")
	void unexpected() throws Exception {
		mockMvc.perform(get("/unexpected"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("COMMON5001"))
				.andExpect(jsonPath("$.error.message").value(CommonErrorCode.INTERNAL_ERROR.getDefaultMessage()));
	}

	@RestController
	static class StubController {

		@GetMapping("/business")
		void business() {
			throw new BusinessException(CommonErrorCode.NOT_FOUND, "대상이 없습니다");
		}

		@PostMapping("/validate")
		void validate(@Valid @RequestBody StubRequest request) {
		}

		@GetMapping("/param")
		void param(@RequestParam Long id) {
		}

		@GetMapping("/unauthenticated")
		void unauthenticated() {
			throw new BadCredentialsException("bad token");
		}

		@GetMapping("/forbidden")
		void forbidden() {
			throw new AccessDeniedException("denied");
		}

		@GetMapping("/integrity-conflict")
		void integrityConflict() {
			throw new DataIntegrityViolationException("duplicate key uk_arrival_sessions_user_active user_id=123");
		}

		@GetMapping("/unexpected")
		void unexpected() {
			throw new IllegalStateException("internal detail");
		}
	}

	record StubRequest(@NotBlank String name) {
	}
}
