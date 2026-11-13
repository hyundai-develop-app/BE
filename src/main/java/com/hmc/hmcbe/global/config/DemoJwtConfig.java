package com.hmc.hmcbe.global.config;

import com.hmc.hmcbe.global.auth.DemoTokenIssuer;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
@Profile({"local", "prod"})
@ConditionalOnProperty(name = "app.demo-auth.enabled", havingValue = "true")
public class DemoJwtConfig {

	@Bean
	KeyPair demoKeyPair() throws NoSuchAlgorithmException {
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	@Bean
	JwtEncoder demoJwtEncoder(KeyPair demoKeyPair) {
		RSAKey key = new RSAKey.Builder((RSAPublicKey) demoKeyPair.getPublic())
			.privateKey((RSAPrivateKey) demoKeyPair.getPrivate()).keyID("hmc-demo").build();
		return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
	}

	@Bean
	JwtDecoder demoJwtDecoder(KeyPair demoKeyPair) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) demoKeyPair.getPublic()).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(DemoTokenIssuer.ISSUER));
		return decoder;
	}

}
