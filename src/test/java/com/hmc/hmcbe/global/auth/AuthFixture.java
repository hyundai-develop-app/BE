package com.hmc.hmcbe.global.auth;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** @WebMvcTest에서 JWT 인증 요청을 만든다. 컨트롤러 테스트는 UserWriteService.resolveUserId(SUBJECT)를 목킹한다. */
public final class AuthFixture {

	public static final String SUBJECT = "issuer-sub-1";
	public static final Long USER_ID = 1L;

	private AuthFixture() {
	}

	public static RequestPostProcessor authenticated() {
		return request -> csrf().postProcessRequest(
				jwt().jwt(token -> token.subject(SUBJECT)).postProcessRequest(request));
	}
}
