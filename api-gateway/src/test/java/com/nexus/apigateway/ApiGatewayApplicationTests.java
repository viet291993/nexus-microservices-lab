package com.nexus.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.test.web.reactive.server.WebTestClient;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApiGatewayApplicationTests {

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void testUnauthorizedRequest_ShouldReturn401() {
		// Gửi 1 request mà không đính kèm Token
		webTestClient.get()
				.uri("/api/v1/users/profile")
				.exchange()
				.expectStatus().isUnauthorized(); // Kỳ vọng Gateway chặn cửa
	}

	@Test
	@SuppressWarnings("null")
	void testAuthorizedRequest_ShouldReturn503Fallback() {
		// Gửi 1 request CÓ Token giả lập (đã qua lớp bảo mật) nhưng Backend chưa dựng
		// sẽ rớt vào Circuit Breaker Fallback (503)
		webTestClient
				.mutateWith(mockJwt()) // Tiêm (Mock) 1 Token hợp lệ vào Spring Security
				.get()
				.uri("/api/v1/users/profile")
				.exchange()
				.expectStatus().is5xxServerError() // Kỳ vọng là lỗi Server do Fallback trả về 503
				.expectBody()
				.jsonPath("$.status").isEqualTo(503)
				.jsonPath("$.error").isEqualTo("Circuit Breaker Opened - User Service Fallback");
	}
}
