package com.nexus.apigateway;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestSecurityConfig.class)
@AutoConfigureWebTestClient
@DisplayName("FallbackController Unit Tests")
class FallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("User Service Fallback phải trả về HTTP 503")
    void testUserServiceFallback_ShouldReturn503() {
        webTestClient.get()
                .uri("/fallback/userServiceFallback")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("User Service Fallback phải trả về JSON body đúng cấu trúc")
    void testUserServiceFallback_ShouldReturnCorrectJsonBody() {
        webTestClient.get()
                .uri("/fallback/userServiceFallback")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Circuit Breaker Opened - User Service Fallback")
                .jsonPath("$.message").isNotEmpty();
    }

    @Test
    @DisplayName("Product Service Fallback phải trả về HTTP 503")
    void testProductServiceFallback_ShouldReturn503() {
        webTestClient.get()
                .uri("/fallback/productServiceFallback")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Product Service Fallback phải trả về JSON body đúng cấu trúc")
    void testProductServiceFallback_ShouldReturnCorrectJsonBody() {
        webTestClient.get()
                .uri("/fallback/productServiceFallback")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Circuit Breaker Opened - Product Service Timeout/Error")
                .jsonPath("$.message").isNotEmpty();
    }
}
