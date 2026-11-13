package com.hmc.hmcbe.global.config;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"local", "prod"})
class DemoCombinedProfilesTokenTest extends DemoTokenTest {
}
