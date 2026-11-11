package com.hmc.hmcbe.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = ProdSecurityConfigTest.Probe.class)
@Import({ProdSecurityConfig.class, ProdSecurityConfigTest.Probe.class})
@ActiveProfiles("prod")
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=")
class ProdSecurityConfigTest {

	@org.springframework.test.context.bean.override.mockito.MockitoBean
	com.hmc.hmcbe.global.auth.CurrentUserIdArgumentResolver resolver;

	@Autowired
	MockMvc mvc;

	@Test
	void publicEndpointsAreAccessible() throws Exception {
		for (String path : new String[] {"/swagger-ui.html", "/swagger-ui/index.html",
			"/api-docs", "/api-docs/swagger-config", "/actuator/health"}) {
			mvc.perform(get(path)).andExpect(status().isOk());
		}
	}

	@Test
	void protectedEndpointsReturnCommonUnauthorizedResponse() throws Exception {
		for (String path : new String[] {"/private", "/actuator/info", "/actuator/health/details"}) {
			mvc.perform(get(path)).andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("COMMON4011"));
		}
	}

	@RestController
	static class Probe {
		@GetMapping({"/swagger-ui.html", "/swagger-ui/index.html", "/api-docs",
			"/api-docs/swagger-config", "/actuator/health", "/private"})
		String endpoint() {
			return "ok";
		}
	}
}
