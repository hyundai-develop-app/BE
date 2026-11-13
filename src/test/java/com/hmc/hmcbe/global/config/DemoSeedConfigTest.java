package com.hmc.hmcbe.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class DemoSeedConfigTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(DemoSeedConfig.class);

	@ParameterizedTest
	@ValueSource(strings = {"test", "local", "prod"})
	@DisplayName("데모 시드를 켜면 프로필에 관계없이 기존 Flyway 위치 뒤에 데모 위치를 추가한다")
	void appendDemoLocationWithoutReplacingExistingLocations(String profile) {
		// given
		FluentConfiguration configuration = Flyway.configure()
				.locations("classpath:db/migration", "classpath:db/extra");

		// when
		contextRunner.withPropertyValues("app.demo-seed.enabled=true", "spring.profiles.active=" + profile)
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context.getBeansOfType(FlywayConfigurationCustomizer.class)).hasSize(1);
					context.getBean(FlywayConfigurationCustomizer.class).customize(configuration);

					// then
					assertThat(Arrays.stream(configuration.getLocations()).map(Location::getDescriptor).toList())
							.containsExactlyInAnyOrder("classpath:db/migration", "classpath:db/extra", "classpath:db/demo");
				});
	}

	@Test
	@DisplayName("데모 시드 설정을 생략하면 Flyway 커스터마이저를 등록하지 않는다")
	void missingPropertyDoesNotRegisterCustomizer() {
		// given / when
		contextRunner.run(context -> {
			// then
			assertThat(context).hasNotFailed();
			assertThat(context.getBeansOfType(FlywayConfigurationCustomizer.class)).isEmpty();
		});
	}

	@Test
	@DisplayName("데모 시드를 끄면 Flyway 커스터마이저를 등록하지 않는다")
	void falsePropertyDoesNotRegisterCustomizer() {
		// given / when
		contextRunner.withPropertyValues("app.demo-seed.enabled=false").run(context -> {
			// then
			assertThat(context).hasNotFailed();
			assertThat(context.getBeansOfType(FlywayConfigurationCustomizer.class)).isEmpty();
		});
	}
}
