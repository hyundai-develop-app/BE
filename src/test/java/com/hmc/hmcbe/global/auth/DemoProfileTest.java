package com.hmc.hmcbe.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.hmc.hmcbe.global.auth.controller.DemoTokenController;
import com.hmc.hmcbe.global.config.LocalDemoSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class DemoProfileTest {

	@ParameterizedTest
	@ValueSource(strings = {"prod", "test", "local,prod", ""})
	@DisplayName("local 단독 환경이 아니면 데모 발급과 검증 빈을 등록하지 않는다")
	void demoBeansAreAbsentOutsideLocal(String profiles) {
		new ApplicationContextRunner()
			.withPropertyValues("spring.profiles.active=" + profiles)
			.withUserConfiguration(LocalDemoSecurityConfig.class, DemoTokenIssuer.class, DemoTokenController.class)
			.run(context -> {
				assertThat(context).hasNotFailed().doesNotHaveBean(LocalDemoSecurityConfig.class)
					.doesNotHaveBean(DemoTokenIssuer.class).doesNotHaveBean(DemoTokenController.class)
					.doesNotHaveBean(JwtEncoder.class).doesNotHaveBean(JwtDecoder.class);
			});
	}
}
