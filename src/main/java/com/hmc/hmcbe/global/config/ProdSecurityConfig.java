package com.hmc.hmcbe.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmc.hmcbe.global.exception.CommonErrorCode;
import com.hmc.hmcbe.global.response.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
@Profile("prod")
public class ProdSecurityConfig {

	@Bean
	SecurityFilterChain prodSecurityFilterChain(HttpSecurity http,
		@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri, ObjectMapper objectMapper)
		throws Exception {
		AuthenticationEntryPoint entryPoint = (request, response, exception) ->
			writeError(response, objectMapper, CommonErrorCode.UNAUTHORIZED);
		AccessDeniedHandler deniedHandler = (request, response, exception) ->
			writeError(response, objectMapper, CommonErrorCode.FORBIDDEN);
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**",
					"/actuator/health").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
		if (StringUtils.hasText(issuerUri)) {
			http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
				.authenticationEntryPoint(entryPoint).accessDeniedHandler(deniedHandler));
		} else {
			log.warn("OAuth2 issuer is empty; protected APIs will return 401 until authentication is configured.");
		}
		return http.build();
	}
	private void writeError(HttpServletResponse response, ObjectMapper mapper, CommonErrorCode code)
		throws IOException {
		response.setStatus(code.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		mapper.writeValue(response.getOutputStream(), ApiResponse.fail(code, code.getDefaultMessage()));
	}
}
