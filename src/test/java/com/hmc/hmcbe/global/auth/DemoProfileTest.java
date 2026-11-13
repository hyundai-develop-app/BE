package com.hmc.hmcbe.global.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.hmc.hmcbe.global.auth.controller.DemoTokenController;
import com.hmc.hmcbe.global.config.DemoJwtConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class DemoProfileTest {

	@Test
	@DisplayName("test 프로필은 설정을 켜도 데모 발급과 검증 빈을 등록하지 않는다")
	void testProfileNeverEnablesDemoBeans() {
		new ApplicationContextRunner()
			.withPropertyValues("spring.profiles.active=test", "app.demo-auth.enabled=true")
			.withUserConfiguration(DemoJwtConfig.class, DemoTokenIssuer.class, DemoTokenController.class)
			.run(context -> {
				assertThat(context).hasNotFailed().doesNotHaveBean(DemoJwtConfig.class)
					.doesNotHaveBean(DemoTokenIssuer.class).doesNotHaveBean(DemoTokenController.class)
					.doesNotHaveBean(JwtEncoder.class).doesNotHaveBean(JwtDecoder.class);
			});
	}

	@ParameterizedTest
	@ValueSource(strings = {"local", "prod", "test", "local,prod", ""})
	@DisplayName("활성화 설정이 없으면 모든 환경에서 데모 발급과 검증 빈을 등록하지 않는다")
	void demoBeansAreAbsentByDefault(String profiles) {
		new ApplicationContextRunner()
			.withPropertyValues("spring.profiles.active=" + profiles)
			.withUserConfiguration(DemoJwtConfig.class, DemoTokenIssuer.class, DemoTokenController.class)
			.run(context -> {
				assertThat(context).hasNotFailed().doesNotHaveBean(DemoJwtConfig.class)
					.doesNotHaveBean(DemoTokenIssuer.class).doesNotHaveBean(DemoTokenController.class)
					.doesNotHaveBean(JwtEncoder.class).doesNotHaveBean(JwtDecoder.class);
			});
	}
}
