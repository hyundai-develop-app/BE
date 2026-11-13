package com.hmc.hmcbe.global.auth;

import com.hmc.hmcbe.global.auth.dto.response.DemoTokenResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
@Profile("local & !prod")
@RequiredArgsConstructor
public class DemoTokenIssuer {

	public static final String ISSUER = "hmc-local-demo";

	private final JwtEncoder jwtEncoder;
	private final Clock clock;

	public DemoTokenResponse issue(String subject) {
		Instant now = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		JwtClaimsSet claims = JwtClaimsSet.builder()
			.issuer(ISSUER).subject(subject).issuedAt(now)
			.expiresAt(now.plus(1, ChronoUnit.HOURS)).build();
		return DemoTokenResponse.from(jwtEncoder.encode(JwtEncoderParameters.from(claims)));
	}
}
