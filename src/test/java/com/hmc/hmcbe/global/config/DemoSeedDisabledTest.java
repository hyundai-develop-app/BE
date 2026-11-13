package com.hmc.hmcbe.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(PostgisContainerConfig.class)
@TestPropertySource(properties = "DEMO_SEED_ENABLED=false")
class DemoSeedDisabledTest {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Flyway flyway;

	@Autowired
	private Environment environment;

	@Test
	@DisplayName("데모 시드 설정을 추가하지 않으면 기본값 false로 스키마만 생성하고 주차장은 적재하지 않는다")
	void defaultConfigurationDoesNotSeedParkingLots() {
		// given / when
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM parking_lots", Integer.class);

		// then
		assertThat(environment.getProperty("app.demo-seed.enabled", Boolean.class)).isFalse();
		assertThat(Arrays.stream(flyway.getConfiguration().getLocations()).map(Location::getDescriptor).toList())
				.contains("classpath:db/migration")
				.doesNotContain("classpath:db/demo");
		assertThat(count).isZero();
	}
}
