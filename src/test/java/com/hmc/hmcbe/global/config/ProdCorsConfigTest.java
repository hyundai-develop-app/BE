package com.hmc.hmcbe.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.hmc.hmcbe.global.auth.CurrentUserIdArgumentResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** 허용 출처가 설정된 prod 보안 체인의 CORS 동작. */
@WebMvcTest(controllers = ProdSecurityConfigTest.Probe.class)
@Import({ProdSecurityConfig.class, ProdSecurityConfigTest.Probe.class})
@ActiveProfiles("prod")
@TestPropertySource(properties =
		"app.cors.allowed-origins=https://app.example.test, https://admin.example.test")
class ProdCorsConfigTest {

	private static final String ALLOWED_ORIGIN = "https://app.example.test";

	@MockitoBean
	CurrentUserIdArgumentResolver resolver;

	@Autowired
	MockMvc mvc;

	@Test
	@DisplayName("허용한 출처의 preflight는 토큰 없이 통과한다")
	void preflightFromAllowedOrigin() throws Exception {
		mvc.perform(options("/private")
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "authorization"));
	}

	@Test
	@DisplayName("쉼표 뒤 공백이 있어도 두 번째 출처를 허용한다")
	void secondOriginIsTrimmed() throws Exception {
		mvc.perform(options("/private")
						.header(HttpHeaders.ORIGIN, "https://admin.example.test")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("401 응답에도 CORS 헤더가 붙어 브라우저가 에러 본문을 읽을 수 있다")
	void unauthorizedResponseCarriesCorsHeader() throws Exception {
		mvc.perform(get("/private").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
				.andExpect(jsonPath("$.error.code").value("COMMON4011"));
	}

	@Test
	@DisplayName("허용하지 않은 출처의 preflight는 거부한다")
	void preflightFromUnknownOrigin() throws Exception {
		mvc.perform(options("/private")
						.header(HttpHeaders.ORIGIN, "https://evil.example.test")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}
}
