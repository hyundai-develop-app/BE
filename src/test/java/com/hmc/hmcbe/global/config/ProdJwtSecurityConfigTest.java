package com.hmc.hmcbe.global.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProdSecurityConfigTest.Probe.class)
@Import({ProdSecurityConfig.class, ProdSecurityConfigTest.Probe.class})
@ActiveProfiles("prod")
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://issuer.example")
class ProdJwtSecurityConfigTest {

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.hmc.hmcbe.global.auth.CurrentUserIdArgumentResolver resolver;

	@Autowired
	MockMvc mvc;

	@MockitoBean
	JwtDecoder decoder;

	@Test
	void validJwtAuthenticatesProtectedRequest() throws Exception {
		when(decoder.decode("valid")).thenReturn(Jwt.withTokenValue("valid")
			.header("alg", "RS256").subject("1").build());
		mvc.perform(get("/private").header("Authorization", "Bearer valid"))
			.andExpect(status().isOk());
	}

	@Test
	void invalidJwtReturnsCommonUnauthorizedResponse() throws Exception {
		when(decoder.decode("invalid")).thenThrow(new BadJwtException("invalid"));
		mvc.perform(get("/private").header("Authorization", "Bearer invalid"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("COMMON4011"));
	}

	@Test
	void missingJwtIsRejectedAndPublicEndpointsRemainAccessible() throws Exception {
		mvc.perform(get("/private")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.error.code").value("COMMON4011"));
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}
}
