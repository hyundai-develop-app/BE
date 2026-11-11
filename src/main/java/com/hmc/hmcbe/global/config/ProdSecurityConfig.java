package com.hmc.hmcbe.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.util.StringUtils;

@Configuration
@Profile("prod")
public class ProdSecurityConfig {

	@Bean
	SecurityFilterChain prodSecurityFilterChain(HttpSecurity http,
		@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri)
		throws Exception {
		http.csrf(csrf -> csrf.disable())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**",
					"/actuator/health").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
		if (StringUtils.hasText(issuerUri)) {
			http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
		}
		return http.build();
	}
}
