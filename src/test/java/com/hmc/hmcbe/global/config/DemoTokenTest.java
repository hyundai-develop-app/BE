package com.hmc.hmcbe.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmc.hmcbe.global.auth.CurrentUserId;
import com.hmc.hmcbe.global.auth.CurrentUserIdArgumentResolver;
import com.hmc.hmcbe.global.auth.DemoTokenIssuer;
import com.hmc.hmcbe.global.auth.controller.DemoTokenController;
import com.hmc.hmcbe.user.service.UserWriteService;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = {DemoTokenController.class, DemoTokenTest.Probe.class})
@Import({ProdSecurityConfig.class, DemoJwtConfig.class, DemoTokenIssuer.class, ClockConfig.class,
	CurrentUserIdArgumentResolver.class, DemoTokenTest.Probe.class})
@ActiveProfiles("local")
@TestPropertySource(properties = {"spring.security.oauth2.resourceserver.jwt.issuer-uri=",
	"app.demo-auth.enabled=true"})
class DemoTokenTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ObjectMapper mapper;
	@Autowired
	JwtDecoder decoder;
	@Autowired
	JwtEncoder encoder;
	@MockitoBean
	UserWriteService userWriteService;

	@Test
	@DisplayName("토큰을 발급하면 실제 서명 검증과 기존 사용자 ID 해석을 거쳐 API를 호출한다")
	void issuedTokenAuthenticatesExistingUserFlow() throws Exception {
		when(userWriteService.resolveUserId("demo-driver")).thenReturn(42L);

		String body = mvc.perform(post("/api/v1/demo/token").contentType(MediaType.APPLICATION_JSON)
			.content("{\"subject\":\"demo-driver\"}"))
			.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andReturn().getResponse().getContentAsString();
		String token = mapper.readTree(body).at("/data/accessToken").asText();

		var jwt = decoder.decode(token);
		assertThat(jwt.getSubject()).isEqualTo("demo-driver");
		assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plusSeconds(3600));
		assertThat(mapper.readTree(body).at("/data/expiresAt").asText())
			.isEqualTo(jwt.getExpiresAt().toString());
		mvc.perform(get("/demo-test/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isOk()).andExpect(content().string("42"));
		verify(userWriteService).resolveUserId("demo-driver");
	}

	@ParameterizedTest
	@ValueSource(strings = {"{}", "{\"subject\":null}", "{\"subject\":\"\"}", "{\"subject\":\"   \"}"})
	@DisplayName("subject가 없거나 비어 있으면 발급을 거절한다")
	void invalidSubjectIsRejected(String body) throws Exception {
		mvc.perform(post("/api/v1/demo/token").contentType(MediaType.APPLICATION_JSON).content(body))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false));
	}

	@Test
	@DisplayName("subject가 255자를 넘으면 발급을 거절한다")
	void longSubjectIsRejected() throws Exception {
		mvc.perform(post("/api/v1/demo/token").contentType(MediaType.APPLICATION_JSON)
			.content(mapper.writeValueAsString(java.util.Map.of("subject", "x".repeat(256)))))
			.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("토큰이 없거나 서명이 잘못되면 인증을 거절한다")
	void missingOrInvalidTokenIsRejected() throws Exception {
		mvc.perform(get("/demo-test/me")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("COMMON4011"));
		mvc.perform(get("/demo-test/me").header("Authorization", "Bearer invalid"))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("COMMON4011"));
	}

	@Test
	@DisplayName("만료되었거나 issuer가 다른 토큰이면 인증을 거절한다")
	void expiredAndWrongIssuerTokensAreRejected() throws Exception {
		Instant now = Instant.now();
		for (JwtClaimsSet claims : new JwtClaimsSet[] {
			JwtClaimsSet.builder().issuer(DemoTokenIssuer.ISSUER).subject("demo-driver")
				.issuedAt(now.minusSeconds(7200)).expiresAt(now.minusSeconds(3600)).build(),
			JwtClaimsSet.builder().issuer("other-issuer").subject("demo-driver")
				.issuedAt(now).expiresAt(now.plusSeconds(3600)).build()}) {
			String token = encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
			mvc.perform(get("/demo-test/me").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("COMMON4011"));
		}
	}

	@Test
	@DisplayName("다른 서버 키로 서명한 토큰이면 인증을 거절한다")
	void otherSigningKeyIsRejected() throws Exception {
		var config = new DemoJwtConfig();
		var issuer = new DemoTokenIssuer(config.demoJwtEncoder(config.demoKeyPair()), java.time.Clock.systemUTC());
		String token = issuer.issue("demo-driver").accessToken();
		mvc.perform(get("/demo-test/me").header("Authorization", "Bearer " + token))
			.andExpect(status().isUnauthorized());
	}

	@RestController
	static class Probe {
		@GetMapping("/demo-test/me")
		Long me(@CurrentUserId Long userId) {
			return userId;
		}
	}
}
