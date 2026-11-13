package com.hmc.hmcbe.global.config;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("prod")
@TestPropertySource(properties = "spring.security.oauth2.resourceserver.jwt.issuer-uri=https://unused-issuer.invalid")
class DemoProdWithIssuerTokenTest extends DemoTokenTest {
}
