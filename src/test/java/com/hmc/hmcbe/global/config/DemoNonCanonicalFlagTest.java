package com.hmc.hmcbe.global.config;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.demo-auth.enabled=yes")
class DemoNonCanonicalFlagTest extends DemoDisabledProdTest {
}
