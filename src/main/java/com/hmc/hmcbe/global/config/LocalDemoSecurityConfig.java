package com.hmc.hmcbe.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmc.hmcbe.global.auth.DemoTokenIssuer;
import com.hmc.hmcbe.global.exception.CommonErrorCode;
import com.hmc.hmcbe.global.response.ApiResponse;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@Profile("local & !prod")
public class LocalDemoSecurityConfig {

	@Bean
	KeyPair demoKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	@Bean
	JwtEncoder demoJwtEncoder(KeyPair demoKeyPair) {
		RSAKey key = new RSAKey.Builder((RSAPublicKey) demoKeyPair.getPublic())
			.privateKey((RSAPrivateKey) demoKeyPair.getPrivate()).keyID("local-demo").build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
	}

	@Bean
	JwtDecoder demoJwtDecoder(KeyPair demoKeyPair) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) demoKeyPair.getPublic()).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(DemoTokenIssuer.ISSUER));
		return decoder;
	}

	@Bean
	SecurityFilterChain localDemoSecurityFilterChain(HttpSecurity http, ObjectMapper mapper) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.cors(Customizer.withDefaults())
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST, "/api/v1/demo/token").permitAll()
				.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**",
					"/actuator/health").permitAll()
				.anyRequest().authenticated())
			.exceptionHandling(errors -> errors
				.authenticationEntryPoint((request, response, exception) ->
					writeError(response, mapper, CommonErrorCode.UNAUTHORIZED)))
			.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
				.authenticationEntryPoint((request, response, exception) ->
					writeError(response, mapper, CommonErrorCode.UNAUTHORIZED))
				.accessDeniedHandler((request, response, exception) ->
					writeError(response, mapper, CommonErrorCode.FORBIDDEN)));
		return http.build();
	}

	@Bean
	CorsConfigurationSource localDemoCorsConfigurationSource(
		@Value("${app.cors.allowed-origins:}") List<String> allowedOrigins) {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(
			allowedOrigins.stream().map(String::trim).filter(StringUtils::hasText).toList());
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private void writeError(HttpServletResponse response, ObjectMapper mapper, CommonErrorCode code)
		throws IOException {
		response.setStatus(code.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		mapper.writeValue(response.getOutputStream(), ApiResponse.fail(code, code.getDefaultMessage()));
	}
}
