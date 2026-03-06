package com.nexus.apigateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder() {
        return token -> Mono.just(
                Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .claim("sub", "test-user")
                        .build()
        );
    }
}

