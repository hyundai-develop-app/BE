package com.hmc.hmcbe;

import com.hmc.hmcbe.global.config.PostgisContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(PostgisContainerConfig.class)
class HmcBeApplicationTests {

	@Test
	void contextLoads() {
	}

}
