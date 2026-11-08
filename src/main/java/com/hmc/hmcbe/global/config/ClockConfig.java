package com.hmc.hmcbe.global.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 시각에 의존하는 도메인 규칙(주차 경과 시간, 도착 세션 종료 등)을 테스트에서 고정할 수 있게 Clock을 주입한다. */
@Configuration
public class ClockConfig {

	@Bean
	public Clock clock() {
		return Clock.systemUTC();
	}
}
