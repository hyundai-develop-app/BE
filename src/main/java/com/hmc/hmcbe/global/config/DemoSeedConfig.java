package com.hmc.hmcbe.global.config;

import java.util.Arrays;
import org.flywaydb.core.api.Location;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DemoSeedConfig {

	@Bean
	@ConditionalOnProperty(name = "app.demo-seed.enabled", havingValue = "true")
	FlywayConfigurationCustomizer demoSeedFlywayConfigurationCustomizer() {
		return configuration -> {
			Location[] locations = configuration.getLocations();
			Location[] demoLocations = Arrays.copyOf(locations, locations.length + 1);
			demoLocations[locations.length] = new Location("classpath:db/demo");
			configuration.locations(demoLocations);
		};
	}
}
