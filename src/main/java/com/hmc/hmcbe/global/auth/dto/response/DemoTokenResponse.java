package com.hmc.hmcbe.global.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.springframework.security.oauth2.jwt.Jwt;

@Schema(description = "데모 토큰")
public record DemoTokenResponse(
		@Schema(description = "JWT. Authorization 헤더에 `Bearer {accessToken}`으로 보낸다.",
				example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJkZW1vLWRyaXZlciJ9.signature")
		String accessToken,
		@Schema(description = "토큰 타입", example = "Bearer")
		String tokenType,
		@Schema(description = "만료 시각(발급 1시간 후)", example = "2026-11-14T10:30:00Z")
		Instant expiresAt) {
	public static DemoTokenResponse from(Jwt jwt) {
		return new DemoTokenResponse(jwt.getTokenValue(), "Bearer", jwt.getExpiresAt());
	}
}
