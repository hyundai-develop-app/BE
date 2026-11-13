package com.hmc.hmcbe.global.auth.dto.response;

import java.time.Instant;
import org.springframework.security.oauth2.jwt.Jwt;

public record DemoTokenResponse(String accessToken, String tokenType, Instant expiresAt) {

	public static DemoTokenResponse from(Jwt jwt) {
		return new DemoTokenResponse(jwt.getTokenValue(), "Bearer", jwt.getExpiresAt());
	}
}
