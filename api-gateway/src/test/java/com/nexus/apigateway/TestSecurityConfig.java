package com.nexus.apigateway;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

@TestConfiguration
public class TestSecurityConfig {

    /**
     * Provides a ReactiveJwtDecoder that produces a Jwt with a fixed header and subject for any token.
     *
     * This decoder is intended for tests and emits a Jwt whose token value equals the provided token,
     * with header "alg" set to "none" and claim "sub" set to "test-user".
     *
     * @return a ReactiveJwtDecoder that emits a Jwt with the input token value, header "alg"="none", and claim "sub"="test-user"
     */
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

