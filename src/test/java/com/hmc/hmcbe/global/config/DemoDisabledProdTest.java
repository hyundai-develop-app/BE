package com.hmc.hmcbe.global.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.hmc.hmcbe.global.auth.CurrentUserIdArgumentResolver;
import com.hmc.hmcbe.global.auth.DemoTokenIssuer;
import com.hmc.hmcbe.global.auth.controller.DemoTokenController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DemoTokenController.class)
@Import({ProdSecurityConfig.class, DemoJwtConfig.class, DemoTokenIssuer.class, DemoTokenController.class})
@ActiveProfiles("prod")
@TestPropertySource(properties = {"app.demo-auth.enabled=false",
	"spring.security.oauth2.resourceserver.jwt.issuer-uri="})
class DemoDisabledProdTest {

	@Autowired
	MockMvc mvc;
	@MockitoBean
	CurrentUserIdArgumentResolver resolver;

	@Test
	@DisplayName("데모 설정을 끄면 발급 경로는 공개되지 않고 컨트롤러도 등록되지 않는다")
	void disabledEndpointIsNotAvailable() throws Exception {
		mvc.perform(post("/api/v1/demo/token").contentType(MediaType.APPLICATION_JSON)
			.content("{\"subject\":\"demo-driver\"}"))
			.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("COMMON4011"));
		mvc.perform(post("/api/v1/demo/token").with(jwt()).contentType(MediaType.APPLICATION_JSON)
			.content("{\"subject\":\"demo-driver\"}"))
			.andExpect(status().isNotFound());
	}
}
